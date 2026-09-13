package com.tsa.api.config;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 认证端点 IP 限频拦截器（计划 B12）：只圈 {@code /tsa/auth/**} 下的三个 POST
 * （wechat-login、verify-phone、admin-login）。
 *
 * <p>为什么要限：verify-phone 在 mock-mode 下是「随便填个手机号就拿到乡会身份」的入口，
 * wechat-login 每次成功都要打一发微信上游，admin-login 则是管理后台的口令爆破面 ——
 * 不设闸口等于把爆破与凭据消耗敞开放给任何脚本。admin-login 阈值最紧（默认 5/分），
 * 且后端对「账号不存在/密码错」统一回 1307 + 计时抹平（见 AdminAuthServiceImpl），
 * 再叠这道 IP 限频，让用户名枚举几乎不可行。
 *
 * <p>实现：Hutool TimedCache，<b>进程内存、固定 60s 窗口</b>（get 传 isUpdateLastAccess=false，
 * 不读续期，窗口自该 IP 首请求起算；窗口边缘理论上可 2 倍速突发 —— 本闸门目标是拦无脑刷，
 * 不做精确计费，可接受）。计数键 = IP + 端点，三个端点各自独立窗口。
 *
 * <p><b>边界与演进</b>（都随二期 Redis 化收口）：
 * <ul>
 *   <li>单实例内存实现：多实例部署时各数各的，实际阈值 ≈ N × 配置值</li>
 *   <li>IP 取 {@code request.getRemoteAddr()}（修订 A8）：直连/dev 下可信；一旦套 nginx 反代，
 *       这里读到的是代理地址，全体用户共享一个桶（10/分 ≈ 全球限流）—— 反代部署<b>必须</b>配
 *       {@code server.forward-headers-strategy}（application.yml 留了注释开关），且仅在
 *       上游代理受信时启用，否则 XFF 可伪造直接绕过限频。故本类不自行解析 X-Forwarded-For</li>
 *   <li>preHandle 里抛 BusinessException 与 Controller 抛出走同一 @RestControllerAdvice 链路，
 *       超限回 {code:1306} 壳（HTTP 200，前端按业务码处理）</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    /** 固定窗口长度：60 秒（阈值语义「次/分钟」与它锁死，改一处即改口径） */
    private static final long WINDOW_MS = 60_000L;
    /** 被限频的三个端点（完整路径，方法另判） */
    private static final String WECHAT_LOGIN_PATH = ApiConstants.BASE_PATH + "/auth/wechat-login";
    private static final String VERIFY_PHONE_PATH = ApiConstants.BASE_PATH + "/auth/verify-phone";
    private static final String ADMIN_LOGIN_PATH = ApiConstants.BASE_PATH + "/auth/admin-login";

    /** 键 = "IP|端点路径"；过期即整桶消失 = 窗口重置 */
    private final TimedCache<String, AtomicInteger> windows = CacheUtil.newTimedCache(WINDOW_MS);

    private final int loginPerMinute;
    private final int verifyPhonePerMinute;
    private final int adminLoginPerMinute;

    public AuthRateLimitInterceptor(
            @Value("${tsa.rate-limit.login-per-minute:10}") int loginPerMinute,
            @Value("${tsa.rate-limit.verify-phone-per-minute:5}") int verifyPhonePerMinute,
            @Value("${tsa.rate-limit.admin-login-per-minute:5}") int adminLoginPerMinute) {
        this.loginPerMinute = loginPerMinute;
        this.verifyPhonePerMinute = verifyPhonePerMinute;
        this.adminLoginPerMinute = adminLoginPerMinute;
    }

    /** 惰性过期只清被读到的键；定期 prune 防 IP 集合无限增长 */
    @PostConstruct
    public void startPrune() {
        windows.schedulePrune(WINDOW_MS);
    }

    @PreDestroy
    public void stopPrune() {
        windows.cancelPruneSchedule();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        int threshold = thresholdFor(request);
        if (threshold <= 0) {
            // /tsa/auth/** 下的其他方法/路径（含将来加的 GET 类公开端点）不设限直接放行
            return true;
        }
        String ip = request.getRemoteAddr();
        String key = ip + '|' + pathWithinApp(request);
        // isUpdateLastAccess=false：只认桶的出生时间 —— 「固定窗口」而非「滑动窗口」的关键
        AtomicInteger count = windows.get(key, false);
        if (count == null) {
            count = new AtomicInteger();
            windows.put(key, count);
        }
        if (count.incrementAndGet() > threshold) {
            log.warn("认证端点限频触发: ip={} path={} 阈值={}/min", ip, pathWithinApp(request), threshold);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
        return true;
    }

    /** 命中被限频的端点则返回其阈值，否则返回 0 表示不限 */
    private int thresholdFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return 0;
        }
        String path = pathWithinApp(request);
        if (WECHAT_LOGIN_PATH.equals(path)) {
            return loginPerMinute;
        }
        if (VERIFY_PHONE_PATH.equals(path)) {
            return verifyPhonePerMinute;
        }
        if (ADMIN_LOGIN_PATH.equals(path)) {
            return adminLoginPerMinute;
        }
        return 0;
    }

    /** 剥掉 contextPath 再比对（本服务默认根路径部署，防将来加 servlet.context-path 后全部失效） */
    private String pathWithinApp(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
