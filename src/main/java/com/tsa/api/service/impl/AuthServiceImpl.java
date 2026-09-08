package com.tsa.api.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.tsa.api.client.WechatClient;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.VerifyPhoneVO;
import com.tsa.api.entity.AssociationMember;
import com.tsa.api.service.AssociationMemberService;
import com.tsa.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 乡会身份服务实现。
 *
 * <p>会话设计：
 * <ul>
 *   <li>loginId 用 {@code "assoc-" + associationMember.getId()} 前缀 —— 二期微信登录
 *       会用 member.id 作 loginId，不带前缀会两个身份体系撞号串号。
 *       注意不能用冒号（"assoc:id" 形式）：Sa-Token 保留冒号用于内部 token 格式拼接，
 *       loginId 含冒号会直接抛 SaTokenException</li>
 *   <li>真实手机号放会话（不随 token 出服务端），详情接口校验通过后取用复核</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_ID_PREFIX = "assoc-";
    private static final String SESSION_KEY_PHONE = "assocPhone";

    private final WechatClient wechatClient;
    private final AssociationMemberService associationMemberService;

    @Override
    public VerifyPhoneVO verifyPhone(String code) {
        String phone = wechatClient.exchangePhone(code);
        AssociationMember assoc = associationMemberService.findByPhone(phone);
        if (assoc == null) {
            // 非乡会用户是正常业务结果（HTTP 200 + verified=false），不是错误 —— 前端据此展示拒绝态
            return VerifyPhoneVO.unverified();
        }

        StpUtil.login(LOGIN_ID_PREFIX + assoc.getId());
        StpUtil.getSession().set(SESSION_KEY_PHONE, phone);

        String name = assoc.getName();
        String role = assoc.getRole();
        log.info("乡会身份核验通过: phone={} name={}", phone, name);
        return VerifyPhoneVO.verified(StpUtil.getTokenValue(), name, role);
    }

    @Override
    public void ensureAssoc(String assocToken) {
        if (assocToken == null || assocToken.isBlank()) {
            throw new BusinessException(ResultCode.ASSOC_MEMBER_ONLY);
        }
        Object loginId;
        try {
            loginId = StpUtil.getLoginIdByToken(assocToken);
        } catch (NotLoginException e) {
            // 关键：token 过期/伪造一律转 1301，绝不能漏成 401 ——
            // 401 会触发前端「清登录态跳我的页」，把身份过期误判成登录失效
            throw new BusinessException(ResultCode.ASSOC_MEMBER_ONLY);
        }
        if (!(loginId instanceof String s) || !s.startsWith(LOGIN_ID_PREFIX)) {
            throw new BusinessException(ResultCode.ASSOC_MEMBER_ONLY);
        }
        // 会话里必须有核验时写入的手机号，缺了视为非法会话（防伪造 loginId 的防御性复核）
        Object phone = StpUtil.getSessionByLoginId(loginId).get(SESSION_KEY_PHONE);
        if (phone == null) {
            throw new BusinessException(ResultCode.ASSOC_MEMBER_ONLY);
        }
    }
}