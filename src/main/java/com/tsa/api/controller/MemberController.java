package com.tsa.api.controller;

import com.tsa.api.common.Result;
import com.tsa.api.config.ApiConstants;
import com.tsa.api.dto.MapMarkerVO;
import com.tsa.api.dto.MemberQuery;
import com.tsa.api.dto.MemberVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.dto.ProvinceStatVO;
import com.tsa.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成员公开读接口：小程序首页/我的页「乡友分布」、列表与地图页共用，官网沿用同一组端点。
 */
@Tag(name = "成员", description = "成员列表、省份统计与地图数据")
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

    @Operation(summary = "省份分布统计", description = "小程序首页/我的页「乡友分布」数据源："
            + "仅统计审核通过成员，按人数降序；公开接口")
    @GetMapping("/stats/province")
    public Result<List<ProvinceStatVO>> statsProvince() {
        return Result.ok(memberService.listProvinceStats());
    }

    @Operation(summary = "地图打点数据", description = "小程序地图页专用：仅返回审核通过且有坐标的成员，字段裁剪到最小。"
            + "字面量路径 map-data 在路由时优先于任何参数化路径，不会被吞")
    @GetMapping("/map-data")
    public Result<List<MapMarkerVO>> mapData() {
        return Result.ok(memberService.listMapMarkers());
    }
}
