package com.tsa.api.client.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.client.WechatClient;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

/**
 * 微信开放平台客户端实现：GET /sns/jscode2session（grant_type=authorization_code）。
 *
 * <p>jscode2session 自带鉴权（appid+secret 直接放 query），不需要 access_token。
 */
@Slf4j
@Component
public class WechatClientImpl implements WechatClient {

    private static final String BASE_URL = "https://api.weixin.qq.com";
    /** login-mock-mode 下的固定 openid：必须常量，严禁由 jsCode 派生（见 exchangeOpenid 内注释） */
    private static final String MOCK_OPENID = "mock-openid-local";
    /** 允许开 mock 开关的环境白名单：之外任一环境（含将来新增的 staging/prod）启动即校验 */
    private static final Set<String> MOCK_ALLOWED_PROFILES = Set.of("dev", "test");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    /** appid/secret 经环境变量注入 */
    private final String appId;
    private final String appSecret;
    /** 登录 mock 开关（键 tsa.wechat.login-mock-mode） */
    private final boolean loginMockMode;
    private final Environment environment;

    public WechatClientImpl(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${tsa.wechat.app-id:}") String appId,
            @Value("${tsa.wechat.app-secret:}") String appSecret,
            @Value("${tsa.wechat.login-mock-mode:false}") boolean loginMockMode) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.appId = appId;
        this.appSecret = appSecret;
        this.loginMockMode = loginMockMode;

        // 显式配超时：默认实现无超时，微信接口卡住会把业务线程拖死
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    /**
     * 启动 fail-fast：非 dev/test 环境只要 mock 开关为 true 就拒绝启动。
     *
     * <p>为什么不是「active 含 prod 才拦」：application.yml 写死 active: dev，仓库也没有
     * application-prod.yml —— 按「含 prod」判断的守卫是<b>永不触发的死代码</b>，生产实际是
     * 部署脚本没设 profile 的 dev 裸跑。改成白名单反面判定：不在 {dev, test} 里就一律拦，
     * 新增环境默认受保护。mock 带上线 = 任何人可伪造任意 openid，身份体系归零。
     */
    @PostConstruct
    void guardMockSwitchesOutsideDev() {
        Set<String> active = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean devLike = active.stream().anyMatch(MOCK_ALLOWED_PROFILES::contains);
        if (!devLike && loginMockMode) {
            throw new IllegalStateException("微信登录 mock 开关（tsa.wechat.login-mock-mode）处于开启状态，"
                    + "而当前激活环境 " + active + " 不属于 " + MOCK_ALLOWED_PROFILES + "，拒绝启动。"
                    + "请把开关置 false，或确认 SPRING_PROFILES_ACTIVE 是否漏配。");
        }
    }

    @Override
    public String exchangeOpenid(String jsCode) {
        if (loginMockMode) {
            // 逃生通道：无凭据（或不想碰真微信）时跳过 jscode2session，回固定 openid 走通登录全链路。
            // 严禁由 jsCode 派生 openid —— wx.login 的 code 每次全新，派生值等于「每次登录都是新人」，
            // wechat_user 埋点与 member 绑定全被打乱。生产环境必须保持 false（见上方启动守卫）。
            log.warn("[login-mock-mode] 返回固定 openid={}，忽略 wx.login code，仅限本地联调！", MOCK_OPENID);
            return MOCK_OPENID;
        }

        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            // 凭据缺失属运维配置问题，对外统一 1303
            log.error("未配置 tsa.wechat.app-id/app-secret（WECHAT_APP_ID/WECHAT_APP_SECRET），无法调用 jscode2session");
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }

        String body;
        try {
            body = restClient.get()
                    .uri("/sns/jscode2session?appid={appid}&secret={secret}&js_code={jsCode}&grant_type=authorization_code",
                            appId, appSecret, jsCode)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("jscode2session 网络异常", e);
            // 网络失败可重试，统一 1303：前端静默重登会重新 wx.login 拿新 code
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }

        Code2SessionResponse resp;
        try {
            resp = objectMapper.readValue(body, Code2SessionResponse.class);
        } catch (JsonProcessingException e) {
            log.error("jscode2session 响应解析失败 body={}", body, e);
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }

        // session_key 刻意不进本 record（官方禁令：不得持久化、不得下发前端，拿到它即可解加密数据）。
        // 成功响应里其实不返 errcode，但按防御写法统一判 errcode≠0 或 openid 空即失败；
        // 40029/40163/40226 等真实 errcode 只进日志，对外一律 1303（防泄露微信侧细节，也省得前端逐码适配）
        if ((resp.errcode() != null && resp.errcode() != 0)
                || resp.openid() == null || resp.openid().isBlank()) {
            log.error("jscode2session 失败 errcode={} errmsg={}", resp.errcode(), resp.errmsg());
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        return resp.openid();
    }

    /** jscode2session 响应（仅反序列化用到的字段；session_key 依官方禁令刻意不声明） */
    private record Code2SessionResponse(
            @JsonProperty("openid") String openid,
            Integer errcode,
            String errmsg) {
    }
}
