package com.tsa.api.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 统一分页响应结构 —— 与前端契约对齐的形状。
 *
 * <p>教学要点：MyBatis-Plus 的 {@link IPage} 序列化出来是
 * records/total/current/size/pages，那是"数据库分页对象"的形状，不是接口契约。
 * 框架内部对象不直接当契约输出——在 dto 层定义自己的分页结构，
 * 用 {@link #of(IPage)} 做一次转换，将来换分页实现也不影响前端。
 *
 * @param <T> 列表元素类型
 */
@Data
@Schema(description = "统一分页结构")
public class PageVO<T> {

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "总记录数", example = "300")
    private long total;

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private long page;

    @Schema(description = "每页条数", example = "20")
    private long pageSize;

    public PageVO(List<T> list, long total, long page, long pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /** 把 MyBatis-Plus 的 IPage 转成契约形状 */
    public static <T> PageVO<T> of(IPage<T> page) {
        return new PageVO<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
