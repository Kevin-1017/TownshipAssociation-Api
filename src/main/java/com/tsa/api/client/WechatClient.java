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

    /**
     * 用 wx.login 的动态 code 换 openid（{@code /sns/jscode2session}）。
     *
     * <p><b>此 code 与 {@link #exchangePhone(String)} 的 code 不通用</b>（呼应上方法警示）：
     * 前者来自 {@code wx.login} 的 success 回调，同样 5 分钟有效、严格一次性，
     * 拿错来源的 code 会被微信判 40029/40163，重试只能重新 wx.login。
     *
     * <p>失败语义：空 code / 凭据未配 / errcode≠0 / openid 为空 / 上游异常，一律抛
     * {@code BusinessException(WECHAT_LOGIN_FAILED)}（1303），真实 errcode 只进日志。
     * 上游返回的 session_key 在实现层就地丢弃——官方明令禁止持久化或下发前端，
     * 本系统不使用加密数据解密能力（计划 v1.1 rejectedIdeas #4 已裁掉 checkSession 辅助轨）。
     *
     * @return 微信 openid（该用户在本小程序下的唯一 ID，直接作为 Sa-Token loginId）
     */
    String exchangeOpenid(String jsCode);
}