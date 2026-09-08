package com.tsa.api.client;

/**
 * 微信开放平台客户端（外部集成层）。
 *
 * <p>对外只暴露业务语义，微信 API 的细节（access_token 换取/缓存、错误码映射、
 * HTTP 调用）全部封在实现里。接口化是为了测试时能打桩，与 Service 层同一原则。
 */
public interface WechatClient {

    /**
     * 用 getPhoneNumber 按钮的动态 code 换真实手机号。
     *
     * <p><b>这个 code 与 wx.login 的 code 不是同一个</b>：前者在
     * {@code <button open-type="getPhoneNumber">} 的 getphonenumber 回调里拿到，
     * 5 分钟有效、严格一次性。校验失败/微信侧异常统一抛
     * {@code BusinessException(WECHAT_PHONE_VERIFY_FAILED)}。
     *
     * @return 纯手机号（不含国码，如 {@code 13800000001}）
     */
    String exchangePhone(String code);
}