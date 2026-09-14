package com.tsa.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.EventAdminQuery;
import com.tsa.api.dto.EventDetailVO;
import com.tsa.api.dto.EventListVO;
import com.tsa.api.dto.EventQuery;
import com.tsa.api.dto.EventSaveRequest;
import com.tsa.api.dto.PageVO;
import com.tsa.api.entity.Activity;
import com.tsa.api.mapper.ActivityMapper;
import com.tsa.api.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 活动业务实现（v1.2 D2）：activity 表只读投影，展示态实时派生。
 *
 * <p>投影全部手写字段赋值而不是 BeanUtil：实体与 VO 有意<b>异名</b>
 * （coverUrl→cover），copyProperties 会静默漏掉异名字段——正是这种
 * 「拷了个 null 出去」最难查，索性逐字段写明白，白名单一目了然。
 *
 * <p>为什么不过滤库内 status 列（0未开始/1报名中…）：本期不做发布/下架开关（用户定稿
 * 「删除即下架」），发布靠逻辑删除位 deleted（@TableLogic 自动排除）、不碰 status 列；
 * 将来若真做下架工作流，收口加的就是这里一处过滤，不外溢到契约与前端。
 */
@Service
public class EventServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements EventService {

    /** 展示态（契约 C5 字面量）：start_time 晚于此刻 */
    private static final String STATUS_UPCOMING = "upcoming";
    /** 展示态（契约 C5 字面量）：start_time 已到/已过——本期不做「进行中」细分，前端只分「近期/往期」 */
    private static final String STATUS_PAST = "past";

    /** 「此刻」的基准时区：与 JacksonConfig 的序列化时区同源——status 派生和 startTime 展示必须同一个钟 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public PageVO<EventListVO> pageQuery(EventQuery query) {
        // 钳制口径与 MemberServiceImpl 完全一致：防 pageSize=10000 拖垮数据库
        long size = Math.min(Math.max(query.getPageSize() == null ? 10 : query.getPageSize(), 1), 50);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .orderByDesc(Activity::getStartTime);

        // 契约 C5：year（单年=区间退化）与 yearFrom..yearTo（含端点、两侧可缺省=开区间）
        // 同时给出时取交集：下界取最大、上界取最小。越界值直接判非法，而不是让 LocalDate.of
        // 内部抛 DateTimeException 被兜底伪装成 500（B5 拆穿同一陷阱的口径）。
        requireFourDigitYear(query.getYear(), "年份参数不合法");
        requireFourDigitYear(query.getYearFrom(), "yearFrom须为4位数字年份");
        requireFourDigitYear(query.getYearTo(), "yearTo须为4位数字年份");
        if (query.getYearFrom() != null && query.getYearTo() != null
                && query.getYearFrom() > query.getYearTo()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "yearFrom不能大于yearTo");
        }

        Integer lowerYear = null;
        Integer upperYear = null;
        if (query.getYear() != null) {
            lowerYear = query.getYear();
            upperYear = query.getYear();
        }
        if (query.getYearFrom() != null) {
            lowerYear = lowerYear == null ? query.getYearFrom() : Math.max(lowerYear, query.getYearFrom());
        }
        if (query.getYearTo() != null) {
            upperYear = upperYear == null ? query.getYearTo() : Math.min(upperYear, query.getYearTo());
        }
        // 年份边界过滤用 [当年1月1日, 次年1月1日) 半开区间而不是 YEAR(start_time)=? 函数：
        // 函数包住列会让 idx_start_time 失效走全表扫（本期表小无感，钉死的是写法的下界）。
        // year 与区间交出的空集（如 year=2024 + yearFrom=2025）不报错，ge/lt 自然夹出空页。
        if (lowerYear != null) {
            wrapper.ge(Activity::getStartTime, LocalDate.of(lowerYear, 1, 1).atStartOfDay());
        }
        if (upperYear != null) {
            wrapper.lt(Activity::getStartTime, LocalDate.of(upperYear + 1, 1, 1).atStartOfDay());
        }

        return PageVO.of(this.page(new Page<>(page, size), wrapper).convert(this::toListVO));
    }

    @Override
    public EventDetailVO detail(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            // 契约 C6：查无回业务码 1002（与公告详情同款口径），不出 404 壳
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "活动不存在：id=" + id);
        }
        return toDetailVO(activity);
    }

    @Override
    public PageVO<EventListVO> adminPageQuery(EventAdminQuery query) {
        long size = Math.min(Math.max(query.getPageSize() == null ? 10 : query.getPageSize(), 1), 50);
        long page = Math.max(query.getPage() == null ? 1 : query.getPage(), 1);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .like(StringUtils.hasText(query.getKeyword()), Activity::getTitle, query.getKeyword())
                .orderByDesc(Activity::getStartTime);
        return PageVO.of(this.page(new Page<>(page, size), wrapper).convert(this::toListVO));
    }

    @Override
    public Long create(EventSaveRequest request) {
        Activity activity = new Activity();
        applyEditableFields(activity, request);
        // 报名/经纬度/人数/content 等列不进管理端契约，保持库默认；status 列本期不参与对外过滤
        this.save(activity);
        return activity.getId();
    }

    @Override
    public void update(Long id, EventSaveRequest request) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "活动不存在：id=" + id);
        }
        applyEditableFields(activity, request);
        this.updateById(activity);
    }

    @Override
    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "活动不存在：id=" + id);
        }
        // 逻辑删除（@TableLogic）：删除即下架，公开列表/详情/首页速览随即查不到这行
        this.removeById(id);
    }

    /** 可编辑字段的单一写入点（create/update 共用）：VO 的 cover ↔ 实体的 coverUrl 异名，逐字段显式赋值 */
    private void applyEditableFields(Activity activity, EventSaveRequest request) {
        activity.setTitle(request.getTitle());
        activity.setCoverUrl(request.getCover());
        activity.setSummary(request.getSummary());
        activity.setArticleUrl(request.getArticleUrl());
        activity.setStartTime(request.getStartTime());
    }

    /** 实体 → 列表项（契约 C5）：无 content；status 由 start_time 派生，不落库；articleUrl 供 web 列表直跳公众号 */
    private EventListVO toListVO(Activity activity) {
        EventListVO vo = new EventListVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setCover(activity.getCoverUrl());
        vo.setSummary(activity.getSummary());
        vo.setArticleUrl(activity.getArticleUrl());
        vo.setStartTime(activity.getStartTime());
        vo.setStatus(isUpcoming(activity) ? STATUS_UPCOMING : STATUS_PAST);
        return vo;
    }

    /** 实体 → 详情（契约 C6）：列表字段 + articleUrl，唯独没有 content —— 正文在公众号 */
    private EventDetailVO toDetailVO(Activity activity) {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setCover(activity.getCoverUrl());
        vo.setSummary(activity.getSummary());
        vo.setStartTime(activity.getStartTime());
        vo.setArticleUrl(activity.getArticleUrl());
        return vo;
    }

    /** 4 位年份校验：null（未给）放行，越界判 400——错误信息按参数名各自给 */
    private void requireFourDigitYear(Integer year, String message) {
        if (year != null && (year < 1000 || year > 9999)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    /**
     * 「未开始」判定：start_time 晚于此刻。
     *
     * <p>用库时区（Asia/Shanghai，与 JacksonConfig 对齐）取「现在」而不是服务器默认时区：
     * 部署机若跑在 UTC，status 会在每天 8 小时里整体翻转错档——派生字段更要钉死基准。
     */
    private boolean isUpcoming(Activity activity) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        return activity.getStartTime() != null && activity.getStartTime().isAfter(now);
    }
}
