package com.tsa.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tsa.api.common.Result;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberSaveRequest;
import com.tsa.api.entity.Member;
import com.tsa.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "成员分页列表", description = "支持按城市、行业筛选和姓名/简介关键字搜索；仅返回审核通过的成员")
    @GetMapping
    public Result<IPage<Member>> page(MemberQuery query) {
        return Result.ok(memberService.pageQuery(query));
    }

    @Operation(summary = "地图打点数据", description = "小程序地图页专用：仅返回审核通过且有坐标的成员，字段裁剪到最小")
    @GetMapping("/map-data")
    public Result<List<MapMarkerVO>> mapData() {
        return Result.ok(memberService.listMapMarkers());
    }

    @Operation(summary = "成员注册", description = "小程序端填写资料后提交，状态为待审核，由管理后台审核")
    @PostMapping
    public Result<Long> register(@Valid @RequestBody MemberSaveRequest request) {
        Member saved = memberService.register(request);
        return Result.ok(saved.getId());
    }
}
