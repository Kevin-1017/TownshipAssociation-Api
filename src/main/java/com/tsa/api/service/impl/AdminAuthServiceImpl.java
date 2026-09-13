package com.tsa.api.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.AdminCurrentUserVO;
import com.tsa.api.dto.AdminLoginRequest;
import com.tsa.api.dto.AdminLoginVO;
import com.tsa.api.entity.AdminUser;
import com.tsa.api.mapper.AdminUserMapper;
import com.tsa.api.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理后台身份服务实现：账号口令登录 + 当前会话读取 + 注销。
 *
 * <p>会话设计（与 AuthServiceImpl 同一命名空间互斥思路）：
 * <ul>
 *   <li>loginId 用 {@code "admin-" + admin_user.id} —— 连字符前缀是 {@link com.tsa.api.config.StpInterfaceImpl}
 *       判定 "admin" 角色的唯一依据（openid / assoc 会话永不可能长出此前缀），
 *       与 {@code assoc-} 惯例一致，且避开 Sa-Token 对 loginId 含冒号的默认禁令</li>
 *   <li>role/username 写进 SaSession，供 {@code GET /tsa/admin/auth/me} 与角色判定读取（不随令牌出服务端）</li>
 * </ul>
 *
 * <p><b>防账号枚举</b>：账号不存在时同样跑一次 {@link BCrypt#checkpw}（对固定哑哈希）再抛 1307，
 * 使「用户名不存在」与「密码错误」的响应耗时几乎一致 —— 否则不存在的账号秒回、存在的账号要等一次
 * BCrypt 计算，攻击者据时间差即可枚举出合法用户名。三种失败（不存在/密码错/已删除）对外同一个 1307 码、
 * 同一句 message，不留话缝。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    /** 管理后台 loginId 前缀（与 StpInterfaceImpl 各自持有，避免跨包常量耦合） */
    private static final String ADMIN_PREFIX = "admin-";
    /** 会话键：角色 / 用户名（与 StpInterfaceImpl 读端同名约定） */
    private static final String SESSION_KEY_ROLE = "adminRole";
    private static final String SESSION_KEY_USERNAME = "adminUsername";
    /**
     * 哑哈希：仅用于「账号不存在」分支抹平 BCrypt 计时（其值对不对无意义，结果被丢弃）。
     * 是一条合法的 BCrypt 串（cost=10），保证 checkpw 会真正走完一次哈希计算。
     */
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$y1z9OlKXPpVacOMp14R47uLqFz1jnfhmunTBqrHdJFWXL66vl.8S2";

    private final AdminUserMapper adminUserMapper;

    @Override
    public AdminLoginVO login(AdminLoginRequest request) {
        // 查无此人 / 已软删除（@TableLogic 自动过滤 deleted=1）都是 null
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername()));
        if (user == null) {
            // 关键防枚举：账号不存在也要付一次 BCrypt 计算的时间，抹平与「密码错误」分支的计时差异
            BCrypt.checkpw(request.getPassword(), DUMMY_BCRYPT_HASH);
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        StpUtil.login(ADMIN_PREFIX + user.getId());
        SaSession session = StpUtil.getSession();
        session.set(SESSION_KEY_ROLE, user.getRole());
        session.set(SESSION_KEY_USERNAME, user.getUsername());

        // 更新最近登录时间：只带上 id + lastLoginAt，NOT_NULL 策略下其余列（含 passwordHash）不动
        AdminUser loginStamp = new AdminUser();
        loginStamp.setId(user.getId());
        loginStamp.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(loginStamp);

        log.info("管理后台登录成功: username={} role={}", user.getUsername(), user.getRole());
        return AdminLoginVO.of(StpUtil.getTokenValue(), user.getUsername(), user.getRole());
    }

    @Override
    public void logout() {
        // 幂等（同 AuthServiceImpl.logout 语义）：无 token 直接 200，前端清本地态不该被 401 卡住
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null || !String.valueOf(loginId).startsWith(ADMIN_PREFIX)) {
            // 非 admin 会话（openid / assoc）来调 admin-logout：不注销它，幂等静默返回
            return;
        }
        StpUtil.logout();
    }

    @Override
    public AdminCurrentUserVO currentUser() {
        // /tsa/admin/** 的 checkRole("admin") 已在墙外挡掉非管理员；此处对 loginId 前缀做防御性复核
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null || !String.valueOf(loginId).startsWith(ADMIN_PREFIX)) {
            throw new NotLoginException(NotLoginException.DEFAULT_MESSAGE, StpUtil.TYPE,
                    loginId == null ? NotLoginException.NOT_TOKEN : NotLoginException.INVALID_TOKEN);
        }
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        String username = (String) session.get(SESSION_KEY_USERNAME);
        Integer role = (Integer) session.get(SESSION_KEY_ROLE);
        return AdminCurrentUserVO.of(username, role);
    }
}
