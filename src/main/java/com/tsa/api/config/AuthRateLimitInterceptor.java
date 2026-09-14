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
 * 公开写端点 IP 限频拦截器（计划 B12 + 2026-09-14 社区防灌扩展）。
 *
 * <p>覆盖两组端点、两种窗口：
 * <ul>
 *   <li><b>认证组（60 秒固定窗口）</b>：wechat-login 10/分、verify-phone 5/分、admin-login 5/分。
 *       verify-phone 在 mock-mode 下是「随便填个手机号就拿到乡会身份」的入口，wechat-login 每次成功
 *       都要打一发微信上游，admin-login 是管理后台的口令爆破面。admin-login 阈值最紧，
 *       且后端对「账号不存在/密码错」统一回 1307 + 计时抹平（见 AdminAuthServiceImpl），
 *       再叠这道 IP 限频，让用户名枚举几乎不可行。</li>
 *   <li><b>社区写组（1 小时固定窗口，用户定稿 5 次/时/IP）</b>：发布（posts）、点赞（posts/&#42;/like）、
 *       评论（posts/&#42;/comments）、配图上传（uploads）。社区一期无登录、人人可写，
 *       不设闸口等于把「灌审核队列 / 刷赞刷评论 / 匿名刷盘」敞开放给脚本。</li>
 * </ul>
 *
 * <p><b>归一化桶</b>：点赞/评论的路径带帖子 id（/tsa/community/posts/123/like），若拿完整路径当计数键，
 * 脚本换 id 轮询就是每个桶各打一枪、永远不超限。所以计数键 = IP + 桶名（如 "community/like"），
 * 同一动作跨所有帖子共享一个桶 —— 这是本类从「路径精确匹配」升级成「桶归一」的原因。
 *
 * <p>实现：Hutool TimedCache，分钟/小时各一张表（过期即整桶消失=窗口重置），
 * get 传 isUpdateLastAccess=false 不读续期，窗口自该 IP 该桶首请求起算；
 * 窗口边缘理论上可 2 倍速突发——本闸门目标是拦无脑刷,不做精确计费,可接受。
 *
 * <p><b>边界与演进</b>（都随二期 Redis 化收口）：
 * <ul>
 *   <li>单实例内存实现：多实例部署时各数各的，实际阈值 ≈ N × 配置值</li>
 *   <li>IP 取 {@code request.getRemoteAddr()}（修订 A8）：直连/dev 下可信；一旦套 nginx 反代，
 *       这里读到的是代理地址，全体用户共享一个桶——反代部署<b>必须</b>配
 *       {@code server.forward-headers-strategy}（prod env 已配 native），且仅在
 *       上游代理受信时启用，否则 XFF 可伪造直接绕过限频。故本类不自行解析 X-Forwarded-For。
 *       注意校园/运营商 NAT 出口共享公网 IP 的用户群会共同消耗「5 次/时」额度，调阈值时盯这条</li>
 *   <li>preHandle 里抛 BusinessException 与 Controller 抛出走同一 @RestControllerAdvice 链路，
 *       超限回 {code:1306} 壳（HTTP 200，前端按业务码处理）</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    /** 认证组窗口：60 秒（阈值语义「次/分钟」与它锁死） */
    private static final long WINDOW_MINUTE_MS = 60_000L;
    /** 社区写组窗口：1 小时（用户定稿 5 次/时/IP；阈值语义「次/小时」与它锁死） */
    private static final long WINDOW_HOUR_MS = 3_600_000L;

    /** 精确路径（无参数段） */
    private static final String WECHAT_LOGIN_PATH = ApiConstants.BASE_PATH + "/auth/wechat-login";
    private static final String VERIFY_PHONE_PATH = ApiConstants.BASE_PATH + "/auth/verify-phone";
    private static final String ADMIN_LOGIN_PATH = ApiConstants.BASE_PATH + "/auth/admin-login";
    private static final String COMMUNITY_UPLOAD_PATH = ApiConstants.BASE_PATH + "/community/uploads";
    private static final String COMMUNITY_POST_PATH = ApiConstants.BASE_PATH + "/community/posts";
    /** 参数化端点的桶前缀：/tsa/community/posts/{id}/like、.../comments 归一进同一桶 */
    private static final String COMMUNITY_POSTS_PREFIX = ApiConstants.BASE_PATH + "/community/posts/";

    /** 键 = "IP|桶名"；过期即整桶消失 = 窗口重置 */
    private final TimedCache<String, AtomicInteger> minuteWindows = CacheUtil.newTimedCache(WINDOW_MINUTE_MS);
    private final TimedCache<String, AtomicInteger> hourWindows = CacheUtil.newTimedCache(WINDOW_HOUR_MS);

    private final int loginPerMinute;
    private final int verifyPhonePerMinute;
    private final int adminLoginPerMinute;
    private final int communityUploadPerHour;
    private final int communityPostPerHour;
    private final int communityLikePerHour;
    private final int communityCommentPerHour;

    /** 命中结果：桶名（计数键成分）+ 所在窗口表 + 该窗口内允许次数 */
    private record RateRule(String bucket, TimedCache<String, AtomicInteger> windows, int limit) {
    }

    public AuthRateLimitInterceptor(
            @Value("${tsa.rate-limit.login-per-minute:10}") int loginPerMinute,
            @Value("${tsa.rate-limit.verify-phone-per-minute:5}") int verifyPhonePerMinute,
            @Value("${tsa.rate-limit.admin-login-per-minute:5}") int adminLoginPerMinute,
            @Value("${tsa.rate-limit.community-upload-per-hour:5}") int communityUploadPerHour,
            @Value("${tsa.rate-limit.community-post-per-hour:5}") int communityPostPerHour,
            @Value("${tsa.rate-limit.community-like-per-hour:5}") int communityLikePerHour,
            @Value("${tsa.rate-limit.community-comment-per-hour:5}") int communityCommentPerHour) {
        this.loginPerMinute = loginPerMinute;
        this.verifyPhonePerMinute = verifyPhonePerMinute;
        this.adminLoginPerMinute = adminLoginPerMinute;
        this.communityUploadPerHour = communityUploadPerHour;
        this.communityPostPerHour = communityPostPerHour;
        this.communityLikePerHour = communityLikePerHour;
        this.communityCommentPerHour = communityCommentPerHour;
    }

    /** 惰性过期只清被读到的键；定期 prune 防 IP 集合无限增长（两张表同节拍 60s 一剪） */
    @PostConstruct
    public void startPrune() {
        minuteWindows.schedulePrune(WINDOW_MINUTE_MS);
        hourWindows.schedulePrune(WINDOW_MINUTE_MS);
    }

    @PreDestroy
    public void stopPrune() {
        minuteWindows.cancelPruneSchedule();
        hourWindows.cancelPruneSchedule();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateRule rule = ruleFor(request);
        if (rule == null) {
            // 未被圈定的方法/路径（含全部 GET 读端点与将来新增的公开口）不设限直接放行
            return true;
        }
        String ip = request.getRemoteAddr();
        String key = ip + '|' + rule.bucket();
        // isUpdateLastAccess=false：只认桶的出生时间 —— 「固定窗口」而非「滑动窗口」的关键
        AtomicInteger count = rule.windows().get(key, false);
        if (count == null) {
            count = new AtomicInteger();
            rule.windows().put(key, count);
        }
        if (count.incrementAndGet() > rule.limit()) {
            log.warn("端点限频触发: ip={} bucket={} 阈值={}", ip, rule.bucket(), rule.limit());
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
        return true;
    }

    /** 命中被限频的动作则返回其规则，否则 null 表示不限 */
    private RateRule ruleFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = pathWithinApp(request);
        if (WECHAT_LOGIN_PATH.equals(path)) {
            return new RateRule("auth/wechat-login", minuteWindows, loginPerMinute);
        }
        if (VERIFY_PHONE_PATH.equals(path)) {
            return new RateRule("auth/verify-phone", minuteWindows, verifyPhonePerMinute);
        }
        if (ADMIN_LOGIN_PATH.equals(path)) {
            return new RateRule("auth/admin-login", minuteWindows, adminLoginPerMinute);
        }
        if (COMMUNITY_UPLOAD_PATH.equals(path)) {
            return new RateRule("community/upload", hourWindows, communityUploadPerHour);
        }
        // 发布是精确路径；点赞/评论带帖子 id，必须按前后缀归一桶（见类注释）
        if (COMMUNITY_POST_PATH.equals(path)) {
            return new RateRule("community/post", hourWindows, communityPostPerHour);
        }
        if (path.startsWith(COMMUNITY_POSTS_PREFIX) && path.endsWith("/like")) {
            return new RateRule("community/like", hourWindows, communityLikePerHour);
        }
        if (path.startsWith(COMMUNITY_POSTS_PREFIX) && path.endsWith("/comments")) {
            return new RateRule("community/comment", hourWindows, communityCommentPerHour);
        }
        return null;
    }

    /** 剥掉 contextPath 再比对（本服务默认根路径部署，防将来加 servlet.context-path 后全部失效） */
    private String pathWithinApp(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
