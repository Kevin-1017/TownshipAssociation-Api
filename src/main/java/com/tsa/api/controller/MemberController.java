package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberDetailVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.Member;
import com.tsa.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成员接口：小程序地图页/成员列表页与未来官网共用。
 *
 * <p>教学要点（L1~L2 参考模板）：
 * <ul>
 *   <li>Controller 只做三件事：接参、调 Service、包 Result —— 不写业务逻辑</li>
 *   <li>构造器注入（@RequiredArgsConstructor + final），比 @Autowired 字段注入更利于测试</li>
 *   <li>@Valid 触发 MemberSaveRequest 里的校验注解</li>
 * </ul>
 */
@Tag(name = "成员", description = "成员注册、列表与地图数据")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "成员分页列表", description = "支持按省份、城市、行业筛选和姓名/简介关键字搜索；仅返回审核通过的成员")
    @GetMapping
    public Result<PageVO<MemberVO>> page(MemberQuery query) {
        return Result.ok(memberService.pageQuery(query));
    }

    @Operation(summary = "地图打点数据", description = "小程序地图页专用：仅返回审核通过且有坐标的成员，字段裁剪到最小")
    @GetMapping("/map-data")
    public Result<List<MapMarkerVO>> mapData() {
        return Result.ok(memberService.listMapMarkers());
    }

    @Operation(summary = "成员注册", description = "小程序端填写资料后提交，状态为待审核，由管理后台审核；返回新成员 id（字符串形式）")
    @PostMapping
    public Result<String> register(@Valid @RequestBody MemberSaveRequest request) {
        Member saved = memberService.register(request);
        return Result.ok(String.valueOf(saved.getId()));
    }

    @Operation(summary = "成员详情", description = "乡会用户专享：需请求头 X-Assoc-Token（verify-phone 核验成功签发），"
            + "未核验/失效返回 1301；contactVisible=false 时响应剔除联系方式字段")
    @GetMapping("/{id}")
    public Result<MemberDetailVO> detail(@PathVariable Long id,
                                         @RequestHeader(value = "X-Assoc-Token", required = false) String assocToken) {
        // 两个契约注意点：
        // 1. "/map-data" 是字面量路径，Spring 路由时优先于 "/{id}"，两者不冲突
        // 2. 闸门不放 SaRouter 拦截器而在 Service 层：失败码是业务化的 1301 而非 401，
        //    且 standalone MockMvc 测试不含拦截器，业务校验放 Service 才可测
        return Result.ok(memberService.getDetail(id, assocToken));
    }
}
