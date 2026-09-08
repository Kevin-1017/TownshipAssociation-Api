package com.tsa.api.service;

import com.tsa.api.dto.VerifyPhoneVO;

/**
 * 乡会身份服务：手机号核验 + 乡会身份会话签发/校验。
 *
 * <p>一期身份体系与微信登录（二期）解耦：核验成功签发的 Sa-Token 会话
 * 只代表「乡会用户」，参会登录是另一套。两者 loginId 用前缀隔开（assoc- vs 未来的 member-，
 * 不能用冒号 —— Sa-Token 保留冒号用于内部 token 格式）。
 */
public interface AuthService {

    /**
     * 用 getPhoneNumber 动态 code 换手机号并比对乡会名册。
     *
     * <p>命中：签发乡会身份 Sa-Token 会话并返回令牌；未命中：返回
     * <code>verified=false</code>（HTTP 200 业务结果，不是错误）。
     */
    VerifyPhoneVO verifyPhone(String getPhoneNumberCode);

    /**
     * 乡会身份闸门：详情等受限接口先过这道校验。
     *
     * <p>token 缺失/失效/非法一律抛
     * {@code BusinessException(ASSOC_MEMBER_ONLY)}。
     */
    void ensureAssoc(String assocToken);
}