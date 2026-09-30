package com.tsa.api.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsa.api.client.WechatClient;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.LoginVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.ProfileUpdateRequest;
import com.tsa.api.entity.Member;
import com.tsa.api.mapper.MemberMapper;
import com.tsa.api.mapper.WechatUserMapper;
import com.tsa.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * 身份服务实现：微信登录 + 本人档案读写。
 *
 * <p>loginId 用<b>裸 openid</b>：openid 是微信下发的「同一用户 × 本 appid」唯一 ID，
 * 天然不与数字/前缀撞号，无需自造前缀。管理后台会话的 {@code admin-} 前缀由
 * AdminAuthServiceImpl 单独铸造，两个命名空间互斥的判定见 StpInterfaceImpl。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 联系方式可见：是（member.contact_visible 列口径） */
    private static final int CONTACT_VISIBLE = 1;
    /** 国家常量：一期只有国内成员，字段保留是为了二期海外潮籍乡亲不返工 */
    private static final String COUNTRY_DEFAULT = "中国";
    /** 审核状态：待审核（本人建档初始态） */
    private static final int STATUS_PENDING = 0;
    /** 档案来源：本人提交（DDL 注释口径：0 后台种子 / 1 本人提交） */
    private static final int SOURCE_SELF = 1;
    /** 性别缺省：未知（member.gender NOT NULL DEFAULT 0） */
    private static final int GENDER_UNKNOWN = 0;

    private final WechatClient wechatClient;
    // 这里注入 MemberMapper 而严禁注入 MemberService：MemberServiceImpl 反向依赖本接口，
    // 再注一次就构成循环依赖，Spring Boot 默认禁止循环引用、应用直接起不来。Service→Mapper 是合法下行。
    private final MemberMapper memberMapper;
    private final WechatUserMapper wechatUserMapper;

    @Override
    public LoginVO wechatLogin(String jsCode) {
        // 1. code 换 openid：微信侧失败统一 1303，直接短路（限频在拦截器层，不在这）
        String openid = wechatClient.exchangeOpenid(jsCode);

        // 2. 查档案：首登无 member 行是常态，返回 null（不建幽灵行 —— 表列 NOT NULL 且会打死查重）。
        //    @TableLogic 自动过滤 deleted=0，逻辑删除的档案不会回填
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getOpenid, openid));

        // 3. 签发登录会话：loginId=裸 openid；is-concurrent: true + is-share: false → 每次登录新 token
        StpUtil.login(openid);
        String token = StpUtil.getTokenValue();

        // 4. 登录埋点：旁路写入，失败绝不阻断登录 —— 埋点表挂了不代表身份挂了
        try {
            wechatUserMapper.upsertLogin(openid, null);
        } catch (Exception e) {
            log.warn("wechat_user 登录埋点写入失败（不阻断登录）openid={}", openid, e);
        }

        log.info("微信登录成功: openid={} 建档={}", openid, member != null);
        return LoginVO.of(token, member == null ? null : toOwnDetail(member));
    }

    @Override
    public MemberDetailVO currentUser() {
        // currentOpenid 已含未登录 401 判定，查无档案返回 null
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getOpenid, currentOpenid()));
        return member == null ? null : toOwnDetail(member);
    }

    @Override
    public String currentOpenid() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        // loginId 为 null（无 token/token 失效）抛 NotLoginException → GlobalExceptionHandler 转 401 壳，
        // 前端据此静默重登。后端重启即此态
        if (loginId == null) {
            throw new NotLoginException(NotLoginException.DEFAULT_MESSAGE, StpUtil.TYPE,
                    NotLoginException.NOT_TOKEN);
        }
        return String.valueOf(loginId);
    }

    @Override
    public void logout() {
        // 幂等：无 token/token 失效直接 200 —— 前端「清本地态」流程不该被 401 卡住
        if (StpUtil.getLoginIdDefaultNull() == null) {
            return;
        }
        // 只注销当前 token；同 openid 的其他端 token 服务端滞留
        // （is-concurrent 多端在线语义），Redis 化后统一收口
        StpUtil.logout();
    }

    @Override
    public MemberDetailVO updateOwnProfile(ProfileUpdateRequest request) {
        String openid = currentOpenid();
        try {
            return applyProfilePatch(request, openid);
        } catch (DuplicateKeyException e) {
            // 「查无即插」不原子：表单双击/重放可并发发出两发 PUT，都读到 existing==null，
            // 第二发撞 member.uk_openid。重试一次即可自愈——此时行已存在，会走白名单更新分支。
            // 若重试仍撞唯一键，只可能是「秘书处软删过同 openid 的行」（@TableLogic 查询看不见
            // 它，INSERT 却撞 uk_openid），这不是接口能自愈的，给明确文案而不是伪装 500。
            log.warn("本人建档撞 uk_openid，重试一次: openid={}", openid, e);
            try {
                return applyProfilePatch(request, openid);
            } catch (DuplicateKeyException e2) {
                log.error("重试仍撞 uk_openid（疑似软删残留行）: openid={}", openid, e2);
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "资料状态异常，请联系秘书处处理");
            }
        }
    }

    /** 建档（INSERT）或白名单更新（UPDATE）一次；并发撞唯一键的异常由调用方分类处理 */
    private MemberDetailVO applyProfilePatch(ProfileUpdateRequest request, String openid) {
        // openid 唯一来源 = Bearer loginId（currentOpenid 已含未登录 401）；
        // 请求体里没有 openid 字段可自报 —— 防冒充
        Member existing = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getOpenid, openid));

        if (existing == null) {
            // 首次建档（「完善资料提交审核」入口）：待审 + 本人提交；
            // province/city 留 NULL 由秘书处审核时补录（DDL 已放宽可空），不编占位假值
            Member member = new Member();
            copyWhitelist(request, member);
            member.setOpenid(openid);
            member.setStatus(STATUS_PENDING);
            member.setSource(SOURCE_SELF);
            // name 列仍 NOT NULL（成员列表的展示底线）：契约全字段可选，空名以空串落库，
            // 「点击完善资料」引导由前端 displayName 三态负责，不在这层伪装成已建档完整
            if (member.getName() == null) {
                member.setName("");
            }
            if (member.getGender() == null) {
                member.setGender(GENDER_UNKNOWN);
            }
            memberMapper.insert(member);
            log.info("本人提交建档（待审）: openid={}", openid);
        } else if (hasWhitelistField(request)) {
            // 白名单补丁更新：patch 只带 id + 请求里非 null 的白名单列，
            // MP updateById 默认 NOT_NULL 策略按实体字段生成 SET 子句 ——
            // status/province/city/openid/source 根本不在 patch 对象上，结构上就无法被本人接口改动
            Member patch = new Member();
            copyWhitelist(request, patch);
            patch.setId(existing.getId());
            memberMapper.updateById(patch);
        }
        // 空 body（{}）对已有档案不发 UPDATE：updateById 全 null 实体会生成空 SET 子句直接 SQL 报错

        // 一律回读最新行：insert 分支拿 DB 默认值/自增 id/createdAt，update 分支拿合并后的真值
        Member latest = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getOpenid, openid));
        if (latest == null) {
            // 理论上不可达（刚写完的行），留作并发删除的诚实兜底而不是赌 NPE
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "档案不存在，请重新登录后再试");
        }
        return toOwnDetail(latest);
    }

    /**
     * 成员实体 → 本人视角详情。
     *
     * <p><b>不裁 phone/wechatId</b>：本人看自己的联系方式不构成隐私泄露，
     * contactVisible 只约束别人能不能看。实体上这两个字段的 @JsonIgnore 挡不住
     * BeanUtil（反射走 getter）。
     */
    private MemberDetailVO toOwnDetail(Member member) {
        // contactVisible 仍显式转 Boolean：Integer→Boolean 交给 Hutool 隐式转换不如写明白
        MemberDetailVO vo = BeanUtil.copyProperties(member, MemberDetailVO.class, "contactVisible");
        vo.setContactVisible(Integer.valueOf(CONTACT_VISIBLE).equals(member.getContactVisible()));
        vo.setCountry(COUNTRY_DEFAULT);
        if (member.getGraduationYear() != null) {
            // 会龄由届别推算，前端无第二来源
            vo.setSeniority(Year.now().getValue() - member.getGraduationYear());
        }
        return vo;
    }

    /**
     * 白名单拷贝：把请求里<b>非 null</b> 的 8 个可改字段覆写到目标实体。
     *
     * <p>手写逐字段而不是 BeanUtil：白名单的意义就在「点名」——copyProperties 是整类反射拷贝，
     * 请求 DTO 将来加一个字段就会悄悄多改一列，审计时看不出边界在哪。
     */
    private static void copyWhitelist(ProfileUpdateRequest request, Member target) {
        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getGender() != null) {
            target.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            target.setPhone(request.getPhone());
        }
        if (request.getWechatId() != null) {
            target.setWechatId(request.getWechatId());
        }
        if (request.getGraduationYear() != null) {
            target.setGraduationYear(request.getGraduationYear());
        }
        if (request.getMajor() != null) {
            target.setMajor(request.getMajor());
        }
        if (request.getIntro() != null) {
            target.setIntro(request.getIntro());
        }
        if (request.getAvatarUrl() != null) {
            target.setAvatarUrl(request.getAvatarUrl());
        }
    }

    /** 请求是否带了至少一个可改字段：全 null 时跳过 updateById（空 SET 子句会 SQL 报错） */
    private static boolean hasWhitelistField(ProfileUpdateRequest request) {
        return request.getName() != null || request.getGender() != null
                || request.getPhone() != null || request.getWechatId() != null
                || request.getGraduationYear() != null || request.getMajor() != null
                || request.getIntro() != null || request.getAvatarUrl() != null;
    }
}
