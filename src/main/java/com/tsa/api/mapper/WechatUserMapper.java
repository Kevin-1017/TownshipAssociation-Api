package com.tsa.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tsa.api.entity.WechatUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 登录埋点表数据访问层。
 *
 * <p>本期只有一个写入方法 {@link #upsertLogin}：走注解 SQL 而非 MP 通用方法，
 * 因为「查了再插」在高并发下有唯一键冲突窗口，ON DUPLICATE KEY 是 MySQL 原子的。
 */
public interface WechatUserMapper extends BaseMapper<WechatUser> {

    /**
     * 登录埋点 upsert：首次 INSERT（login_count=1、first_login_at=NOW()），
     * 命中 uk_openid 唯一键则只刷最近登录时间与计数 ——
     * first_login_at 在 UPDATE 分支<b>故意不列</b>，保住「新增用户」口径不被覆盖。
     *
     * <p>调用方（AuthServiceImpl#wechatLogin）用 try/catch 包住：埋点是旁路，失败不阻断登录。
     *
     * @param unionid 本期恒传 null；jscode2session 仅在绑定开放平台后才返回 unionid，留列备将来
     */
    // unionid 显式 jdbcType=VARCHAR：本期恒传 null，MyBatis 对 null 参数默认按 OTHER 型
    // setNull，部分驱动直接报错 —— 提前钉死类型，将来回填 unionid 时也不用再动这里
    @Insert("INSERT INTO wechat_user (openid, unionid, first_login_at, last_login_at, login_count) "
            + "VALUES (#{openid}, #{unionid,jdbcType=VARCHAR}, NOW(), NOW(), 1) "
            + "ON DUPLICATE KEY UPDATE last_login_at = NOW(), login_count = login_count + 1")
    int upsertLogin(@Param("openid") String openid, @Param("unionid") String unionid);
}
