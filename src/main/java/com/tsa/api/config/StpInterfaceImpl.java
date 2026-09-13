package com.tsa.api.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色/权限数据源（v1.3）：/tsa/admin/** 的 checkRole("admin") 靠本类判定。
 *
 * <p><b>隔离边界就是 loginId 前缀</b>：本项目三套会话共用同一个 Sa-Token，
 * 靠 loginId 命名空间互斥划界——
 * <ul>
 *   <li>微信会员：裸 openid（AuthServiceImpl）</li>
 *   <li>乡会身份：{@code assoc-} 前缀（AuthServiceImpl）</li>
 *   <li>管理后台：{@code admin-} 前缀（AdminAuthServiceImpl，与 assoc- 同为连字符命名空间）</li>
 * </ul>
 * openid/assoc 会话<b>永远不可能</b>凭空长出 {@code admin-} 前缀（前缀只由后台登录接口铸造），
 * 故本类只对 {@code admin-} 前缀发放 {@code admin} 角色、其余一律空列表 —— 这就是「openid 令牌
 * 拿去访问 /tsa/admin/** 被判 NotRoleException(403)」的落点，冒充闭环在此收口。
 *
 * <p>本期不做超管/普管的<b>行为差异</b>（role 仅存/仅回显），故 {@code super} 角色虽在此发放，
 * 尚无任何端点据此裁决；预留给后续多角色收口。权限码本期不用（返回空）。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /** 管理后台 loginId 前缀（与 AdminAuthServiceImpl 各自持有，避免跨包常量耦合——同 AuthServiceImpl 惯例） */
    private static final String ADMIN_PREFIX = "admin-";
    /** 角色：超级管理员（role 列口径 1 超管 / 2 普通） */
    private static final int ROLE_SUPER = 1;
    /** 会话键：登录时写入的角色值（与 AdminAuthServiceImpl 同名约定） */
    private static final String SESSION_KEY_ROLE = "adminRole";

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null || !String.valueOf(loginId).startsWith(ADMIN_PREFIX)) {
            // 微信 openid / assoc- 乡会令牌：不是管理员，空角色 → checkRole("admin") 抛 NotRoleException(403)
            return Collections.emptyList();
        }
        List<String> roles = new ArrayList<>(2);
        roles.add("admin");
        // 仅当会话里 adminRole==1（超管）才追加 super；普管或读不到会话都只回 ["admin"]
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session != null) {
            Object role = session.get(SESSION_KEY_ROLE);
            if (role instanceof Integer && (Integer) role == ROLE_SUPER) {
                roles.add("super");
            }
        }
        return roles;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本期无细粒度权限，闸门只用角色（checkRole）
        return Collections.emptyList();
    }
}
