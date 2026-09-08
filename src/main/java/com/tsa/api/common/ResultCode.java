package com.tsa.api.common;

/**
 * 全局响应状态码。
 *
 * <p>编号规则（教学约定）：
 * <ul>
 *   <li>2xx：成功</li>
 *   <li>4xx：客户端问题（参数、权限）</li>
 *   <li>5xx：服务端问题</li>
 *   <li>1xxx：业务自定义错误</li>
 * </ul>
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

    /** 业务错误：成员 openid 已注册 */
    MEMBER_ALREADY_EXISTS(1001, "该微信已注册过成员"),
    /** 业务错误：通用数据不存在 */
    DATA_NOT_FOUND(1002, "数据不存在"),

    /** 业务错误（13xx 乡会身份/鉴权）：非乡会用户访问受限资料；详情接口未核验/token 失效也返此码（而非 401，避免前端误触发"清登录态跳转"） */
    ASSOC_MEMBER_ONLY(1301, "查看资料仅限乡会会员"),
    /** 业务错误（13xx 乡会身份/鉴权）：微信手机号核验失败（code 无效/已消费/微信侧限频等），可重试 */
    WECHAT_PHONE_VERIFY_FAILED(1302, "手机号核验失败，请重试");

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
