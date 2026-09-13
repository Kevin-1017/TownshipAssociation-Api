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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 微信开放平台客户端实现。
 *
 * <p>三个调用的官方文档：
 * <ul>
 *   <li>access_token：GET /cgi-bin/token（grant_type=client_credential，7200s 有效）</li>
 *   <li>换手机号：POST /wxa/business/getuserphonenumber?access_token=（body 传 code）</li>
 *   <li>换 openid：GET /sns/jscode2session（不需 access_token，登录链路专用）</li>
 * </ul>
 *
 * <p>关键约束：getuserphonenumber 要求小程序为<b>已微信认证的非个人主体</b>。
 * 未认证前本地联调走 mock-mode（把 code 原样当手机号），仅限开发环境。
 */
@Slf4j
@Component
public class WechatClientImpl implements WechatClient {

    private static final String BASE_URL = "https://api.weixin.qq.com";
    /** access_token 提前 60s 视为过期，防边界时刻拿到「下一秒就失效」的 token */
    private static final long REFRESH_AHEAD_MS = 60_000L;
    /** 微信错误码：access_token 无效/过期（清缓存重取后重试一次） */
    private static final Set<Integer> TOKEN_INVALID_CODES = Set.of(40001, 40014, 42001);
    /** 微信错误码：换号 code 无效 / 已被消费（重试无意义，引导用户重新点授权按钮） */
    private static final Set<Integer> CODE_INVALID_CODES = Set.of(40029, 40163);
    /** login-mock-mode 下的固定 openid：必须常量，严禁由 jsCode 派生（见 exchangeOpenid 内注释） */
    private static final String MOCK_OPENID = "mock-openid-local";
    /** 允许开 mock 开关的环境白名单：之外任一环境（含将来新增的 staging/prod）启动即校验 */
    private static final Set<String> MOCK_ALLOWED_PROFILES = Set.of("dev", "test");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    /** appid/secret 经环境变量注入；mock-mode 供本地联调（生产严禁 true） */
    private final String appId;
    private final String appSecret;
    private final boolean mockMode;
    /** 登录 mock 开关：与 mockMode 独立，可只开其一各测各的（键 tsa.wechat.login-mock-mode） */
    private final boolean loginMockMode;
    private final Environment environment;

    /** access_token 是全局单例级缓存（同 appid 全部 API 共享），不是会话数据 —— 内存缓存即可，无需 Redis */
    private volatile String cachedToken;
    private volatile long tokenExpiresAtMs;

    public WechatClientImpl(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${tsa.wechat.app-id:}") String appId,
            @Value("${tsa.wechat.app-secret:}") String appSecret,
            @Value("${tsa.wechat.mock-mode:false}") boolean mockMode,
            @Value("${tsa.wechat.login-mock-mode:false}") boolean loginMockMode) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.appId = appId;
        this.appSecret = appSecret;
        this.mockMode = mockMode;
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
     * 启动 fail-fast（计划修订 A6 口径）：非 dev/test 环境只要任一 mock 开关为 true 就拒绝启动。
     *
     * <p>为什么不是「active 含 prod 才拦」：application.yml 写死 active: dev，仓库也没有
     * application-prod.yml —— 按「含 prod」判断的守卫是<b>永不触发的死代码</b>，生产实际是
     * 部署脚本没设 profile 的 dev 裸跑。改成白名单反面判定：不在 {dev, test} 里就一律拦，
     * 新增环境默认受保护。mock 带上线 = 任何人可伪造任意手机号/openid，身份体系归零。
     */
    @PostConstruct
    void guardMockSwitchesOutsideDev() {
        Set<String> active = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean devLike = active.stream().anyMatch(MOCK_ALLOWED_PROFILES::contains);
        if (!devLike && (mockMode || loginMockMode)) {
            throw new IllegalStateException("微信 mock 开关（tsa.wechat.mock-mode / login-mock-mode）处于开启状态，"
                    + "而当前激活环境 " + active + " 不属于 " + MOCK_ALLOWED_PROFILES + "，拒绝启动。"
                    + "请把两个开关置 false，或确认 SPRING_PROFILES_ACTIVE 是否漏配。");
        }
    }

    @Override
    public String exchangePhone(String code) {
        if (mockMode) {
            // 逃生通道：未认证企业主体时无法调真实换号接口，把 code 原样当手机号。
            // 生产环境必须保持 false，否则任何人都能伪造任意手机号身份。
            log.warn("[mock-mode] 把换号 code 原样当手机号返回，仅限本地联调！");
            return code;
        }

        String token = getAccessToken();
        PhoneResponse resp = callExchange(token, code);
        if (resp.errcode() != null && TOKEN_INVALID_CODES.contains(resp.errcode())) {
            // access_token 失效：清缓存重取后重试一次（能自愈的失败只重试这一种）
            log.warn("微信换号 access_token 失效 errcode={}，重取后重试一次", resp.errcode());
            cachedToken = null;
            resp = callExchange(getAccessToken(), code);
        }
        return requirePhone(resp, code);
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
            // 凭据缺失属运维配置问题（对照 getAccessToken 的同款日志），对外统一 1303
            log.error("未配置 tsa.wechat.app-id/app-secret（WECHAT_APP_ID/WECHAT_APP_SECRET），无法调用 jscode2session");
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }

        String body;
        try {
            // code2session 自带鉴权（appid+secret 直接放 query），不需要 access_token ——
            // 所以这里刻意不经过 getAccessToken() 的缓存链路，登录也就不会被 token 失效问题连坐
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

        // session_key 就地丢弃：微信官方禁令 —— 不得持久化、不得下发前端（拿到它即可解加密数据）。
        // 成功响应里其实不返 errcode，但按防御写法统一判 errcode≠0 或 openid 空即失败；
        // 40029/40163/40226 等真实 errcode 只进日志，对外一律 1303（契约）
        if ((resp.errcode() != null && resp.errcode() != 0)
                || resp.openid() == null || resp.openid().isBlank()) {
            log.error("jscode2session 失败 errcode={} errmsg={}", resp.errcode(), resp.errmsg());
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        return resp.openid();
    }

    // ---------- 微信 API 调用 ----------

    private PhoneResponse callExchange(String token, String code) {
        String body;
        try {
            body = restClient.post()
                    .uri("/wxa/business/getuserphonenumber?access_token={token}", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("code", code))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("微信换号接口网络异常", e);
            // 网络失败可重试，统一 1302：前端停留授权按钮，用户重点即可
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }
        try {
            return objectMapper.readValue(body, PhoneResponse.class);
        } catch (JsonProcessingException e) {
            log.error("微信换号响应解析失败 body={}", body, e);
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }
    }

    /** 校验换号结果并取纯手机号；微信侧 errcode 只进日志，对外统一 1302 中文提示 */
    private String requirePhone(PhoneResponse resp, String code) {
        if ((resp.errcode() == null || resp.errcode() == 0)
                && resp.phoneInfo() != null && resp.phoneInfo().purePhoneNumber() != null
                && !resp.phoneInfo().purePhoneNumber().isBlank()) {
            return resp.phoneInfo().purePhoneNumber();
        }
        if (resp.errcode() != null && CODE_INVALID_CODES.contains(resp.errcode())) {
            log.warn("换号 code 无效或已被消费 errcode={}（前端需引导用户重新点授权按钮）", resp.errcode());
        } else {
            log.warn("微信换号失败 errcode={} errmsg={}", resp.errcode(), resp.errmsg());
        }
        throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
    }

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAtMs - REFRESH_AHEAD_MS) {
            return cachedToken;
        }
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.error("未配置 tsa.wechat.app-id/app-secret（WECHAT_APP_ID/WECHAT_APP_SECRET），无法调用微信接口");
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }

        String body;
        try {
            body = restClient.get()
                    .uri("/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}",
                            appId, appSecret)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("获取 access_token 网络异常", e);
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }

        AccessTokenResponse resp;
        try {
            resp = objectMapper.readValue(body, AccessTokenResponse.class);
        } catch (JsonProcessingException e) {
            log.error("获取 access_token 响应解析失败 body={}", body, e);
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }
        if (resp.accessToken() == null || resp.accessToken().isBlank()) {
            // 40013 invalid appid / 40125 invalid appsecret 属于运维配置问题，日志打 errcode 便于排查
            log.error("获取 access_token 失败 errcode={} errmsg={}", resp.errcode(), resp.errmsg());
            throw new BusinessException(ResultCode.WECHAT_PHONE_VERIFY_FAILED);
        }

        long expiresIn = resp.expiresIn() != null ? resp.expiresIn() : 7200;
        cachedToken = resp.accessToken();
        tokenExpiresAtMs = now + expiresIn * 1000L;
        return cachedToken;
    }

    // ---------- 微信响应结构（仅反序列化用到的字段） ----------

    private record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Integer expiresIn,
            Integer errcode,
            String errmsg) {
    }

    private record PhoneInfo(
            @JsonProperty("purePhoneNumber") String purePhoneNumber) {
    }

    private record PhoneResponse(
            @JsonProperty("phone_info") PhoneInfo phoneInfo,
            Integer errcode,
            String errmsg) {
    }

    /**
     * jscode2session 响应（仅反序列化用到的字段）。
     *
     * <p>sessionKey 声明在此只为完整反序列化，<b>零消费方</b>：官方禁令不得持久化/下发，
     * exchangeOpenid 取完 openid 即整个丢弃。后续任何人不得把它带出本方法。
     */
    private record Code2SessionResponse(
            @JsonProperty("openid") String openid,
            @JsonProperty("session_key") String sessionKey,
            Integer errcode,
            String errmsg) {
    }
}