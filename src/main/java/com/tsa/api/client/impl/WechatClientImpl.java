package com.tsa.api.client.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.client.WechatClient;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 微信开放平台客户端实现。
 *
 * <p>两个调用的官方文档：
 * <ul>
 *   <li>access_token：GET /cgi-bin/token（grant_type=client_credential，7200s 有效）</li>
 *   <li>换手机号：POST /wxa/business/getuserphonenumber?access_token=（body 传 code）</li>
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

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    /** appid/secret 经环境变量注入；mock-mode 供本地联调（生产严禁 true） */
    private final String appId;
    private final String appSecret;
    private final boolean mockMode;

    /** access_token 是全局单例级缓存（同 appid 全部 API 共享），不是会话数据 —— 内存缓存即可，无需 Redis */
    private volatile String cachedToken;
    private volatile long tokenExpiresAtMs;

    public WechatClientImpl(
            ObjectMapper objectMapper,
            @Value("${tsa.wechat.app-id:}") String appId,
            @Value("${tsa.wechat.app-secret:}") String appSecret,
            @Value("${tsa.wechat.mock-mode:false}") boolean mockMode) {
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.mockMode = mockMode;

        // 显式配超时：默认实现无超时，微信接口卡住会把业务线程拖死
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
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
}