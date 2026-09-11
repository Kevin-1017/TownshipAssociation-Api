package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.CommunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CommunityController standalone MockMvc 测试（不连数据库）。 */
class CommunityControllerTest {

    private CommunityService communityService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        communityService = Mockito.mock(CommunityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommunityController(communityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CommunityPostVO sampleVO() {
        CommunityPostVO vo = new CommunityPostVO();
        vo.setId(1L);
        vo.setType("food");
        vo.setAuthor("陈阿姨");
        vo.setTitle("潮汕牛肉丸哪家最正宗?");
        vo.setContent("求推荐地道好店");
        vo.setImages(List.of());
        vo.setLikes(128);
        vo.setComments(36);
        vo.setPublishTime(LocalDateTime.of(2026, 9, 8, 10, 30));
        return vo;
    }

    @Test
    @DisplayName("GET /community/posts - 分页响应应为契约形状且 id 序列化为字符串")
    void pageShouldReturnContractShape() throws Exception {
        PageVO<CommunityPostVO> pageVO = new PageVO<>(List.of(sampleVO()), 1L, 1L, 20L);
        Mockito.when(communityService.pageQuery(ArgumentMatchers.any(CommunityPostQuery.class))).thenReturn(pageVO);

        mockMvc.perform(get("/tsa/community/posts").param("type", "food").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value("1"))
                .andExpect(jsonPath("$.data.list[0].likes").value(128))
                .andExpect(jsonPath("$.data.total").value(1))
                // 列表不返回评论树
                .andExpect(jsonPath("$.data.list[0].commentsList").doesNotExist());
    }

    @Test
    @DisplayName("GET /community/posts/{id} - 详情应带 commentsList")
    void detailShouldIncludeCommentsList() throws Exception {
        CommunityPostVO vo = sampleVO();
        CommentVO c = new CommentVO();
        c.setId(9L);
        c.setAuthor("老郑");
        c.setContent("潮香居绝了");
        c.setLikes(5);
        c.setCreateTime(LocalDateTime.of(2026, 9, 8, 11, 0));
        vo.setCommentsList(List.of(c));
        Mockito.when(communityService.getDetail(1L)).thenReturn(vo);

        mockMvc.perform(get("/tsa/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentsList[0].id").value("9"))
                .andExpect(jsonPath("$.data.commentsList[0].author").value("老郑"));
    }

    @Test
    @DisplayName("GET /community/posts/{id} - 不存在（Service 抛 1002）应返回业务错误码")
    void detailShouldReturn1002WhenNotFound() throws Exception {
        Mockito.when(communityService.getDetail(999L))
                .thenThrow(new BusinessException(ResultCode.DATA_NOT_FOUND));

        mockMvc.perform(get("/tsa/community/posts/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("POST /community/posts - 标题过短应返回 400 并带字段提示")
    void publishShouldFailWhenTitleTooShort() throws Exception {
        CommunityPostSaveRequest req = new CommunityPostSaveRequest();
        req.setType("campus");
        req.setAuthor("张三");
        req.setTitle("短");
        req.setContent("内容内容内容内容内容内容内容内容");

        mockMvc.perform(post("/tsa/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("标题")));
    }

    @Test
    @DisplayName("POST /community/posts - 发布成功应返回字符串 id")
    void publishShouldReturnStringId() throws Exception {
        Mockito.when(communityService.publish(ArgumentMatchers.any())).thenReturn(123L);
        CommunityPostSaveRequest req = new CommunityPostSaveRequest();
        req.setType("campus");
        req.setAuthor("张三");
        req.setTitle("毕业十周年同学会");
        req.setContent("下月在上游举办,欢迎老乡参加");

        mockMvc.perform(post("/tsa/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("123"));
    }

    @Test
    @DisplayName("POST /community/posts/{id}/like - 返回点赞后的总数")
    void likeShouldReturnNewCount() throws Exception {
        Mockito.when(communityService.like(1L)).thenReturn(129);

        mockMvc.perform(post("/tsa/community/posts/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(129));
    }
}
