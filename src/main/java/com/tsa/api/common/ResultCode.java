package com.tsa.api.common;

/**
 * 全局响应状态码：2xx 成功、4xx 客户端问题、5xx 服务端问题、1xxx 业务自定义错误。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    /** 参数校验失败（@Valid 不通过） */
    BAD_REQUEST(400, "请求参数有误"),
    /** 未登录 / token 失效 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** 已登录但无权限 */
    FORBIDDEN(403, "没有操作权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 服务器内部错误（兜底） */
    ERROR(500, "服务器繁忙，请稍后重试"),

    /** 业务错误：通用数据不存在 */
    DATA_NOT_FOUND(1002, "数据不存在"),

    /**
     * 业务错误（13xx 鉴权/限频）：微信登录失败统一码 —— jscode2session 的 code 无效(40029)/已消费(40163)/
     * 被风控(40226)/凭据未配/上游异常全部收敛到此码，真实 errcode 只进日志不外泄（防泄露微信侧细节，
     * 也省得前端逐码适配）。可重试：前端 401 自愈会重新 wx.login 拿新 code。
     */
    WECHAT_LOGIN_FAILED(1303, "微信登录失败，请重试"),
    /**
     * 业务错误（13xx 鉴权/限频）：/tsa/auth/** 按 IP 限频超限（60s 固定窗口）。
     * 刻意不并入 401：限频是「稍后再试」不是「登录失效」，前端据此只 toast 不清登录态。
     * （1301/1302/1304/1305 号段已随乡会核验链下线腾出，勿复用）
     */
    TOO_MANY_REQUESTS(1306, "操作过于频繁，请稍后再试"),
    /**
     * 业务错误（13xx 鉴权/限频）：管理后台登录失败统一码 —— 账号不存在 / 密码错误 / 已删除
     * 三种情形一律收敛到此码（且 message 不带任何区分线索），不给攻击者用响应差异探测
     * 「某用户名是否存在」留话缝（防账号枚举）。
     */
    ADMIN_LOGIN_FAILED(1307, "用户名或密码错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
