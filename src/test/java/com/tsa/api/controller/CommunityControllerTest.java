package com.tsa.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsa.api.common.BusinessException;
import com.tsa.api.common.GlobalExceptionHandler;
import com.tsa.api.common.ResultCode;
import com.tsa.api.dto.CommentVO;
import com.tsa.api.dto.CommunityPostAdminQuery;
import com.tsa.api.dto.CommunityPostQuery;
import com.tsa.api.dto.CommunityPostSaveRequest;
import com.tsa.api.dto.CommunityPostVO;
import com.tsa.api.dto.PageVO;
import com.tsa.api.service.CommunityService;
import com.tsa.api.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CommunityController 与 AdminCommunityController 的 standalone MockMvc 测试（不连数据库）。 */
class CommunityControllerTest {

    private CommunityService communityService;
    private FileStorageService fileStorageService;
    private MockMvc mockMvc;
    private MockMvc adminMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        communityService = Mockito.mock(CommunityService.class);
        fileStorageService = Mockito.mock(FileStorageService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommunityController(communityService, fileStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminMockMvc = MockMvcBuilders
                .standaloneSetup(new AdminCommunityController(communityService))
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
        vo.setStatus(1);
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

    @Test
    @DisplayName("POST /community/uploads - 白名单类型应存盘并回 /tsa/files/<uuid>.<ext> 路径")
    void uploadShouldReturnPathWhenTypeAllowed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[]{1, 2, 3});
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        Mockito.when(fileStorageService.upload(nameCap.capture(),
                        ArgumentMatchers.eq("image/png"), ArgumentMatchers.any()))
                .thenAnswer(inv -> "/tsa/files/" + nameCap.getValue());

        mockMvc.perform(multipart("/tsa/community/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.path").value(
                        org.hamcrest.Matchers.matchesPattern(
                                "^/tsa/files/[0-9a-f-]{36}\\.png$")));
    }

    @Test
    @DisplayName("POST /community/uploads - 类型不在白名单应回 400 统一文案（不存盘）")
    void uploadShouldRejectDisallowedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.html", "text/html", "<script/>".getBytes());

        mockMvc.perform(multipart("/tsa/community/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("文件超限或类型不支持"));
        Mockito.verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("POST /community/uploads - 超过 2MB 业务闸应回 400（容器闸 3MB 在前，此处模拟合法 body）")
    void uploadShouldRejectOversize() throws Exception {
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        big[0] = 1;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        mockMvc.perform(multipart("/tsa/community/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("文件超限或类型不支持"));
        Mockito.verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("POST /community/posts - images 传 2 张应被 @Size(max=1) 拒掉（一图上限）")
    void publishShouldRejectMoreThanOneImage() throws Exception {
        CommunityPostSaveRequest req = new CommunityPostSaveRequest();
        req.setType("food");
        req.setAuthor("陈阿姨");
        req.setTitle("潮汕牛肉丸探店合集");
        req.setContent("内容内容内容内容内容内容内容内容");
        req.setImages(List.of("/tsa/files/a.png", "/tsa/files/b.png"));

        mockMvc.perform(post("/tsa/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1 张")));
    }

    @Test
    @DisplayName("GET /admin/community/posts - status/type 应绑定进管理端查询对象透传")
    void adminPageShouldBindQuery() throws Exception {
        Mockito.when(communityService.adminPageQuery(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.of(sampleVO()), 1L, 1L, 20L));

        adminMockMvc.perform(get("/tsa/admin/community/posts").param("status", "0").param("type", "food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].status").value(1));

        ArgumentCaptor<CommunityPostAdminQuery> captor =
                ArgumentCaptor.forClass(CommunityPostAdminQuery.class);
        Mockito.verify(communityService).adminPageQuery(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals("food", captor.getValue().getType());
    }

    @Test
    @DisplayName("PUT /admin/community/posts/{id}/audit - 通过应透传 status=1 并回 Void")
    void auditShouldPassThroughApprove() throws Exception {
        adminMockMvc.perform(put("/tsa/admin/community/posts/7/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
        Mockito.verify(communityService).audit(7L, 1);
    }

    @Test
    @DisplayName("PUT /admin/community/posts/{id}/audit - status=5 越界应被 @Min/@Max 拒（不落 Service）")
    void auditShouldRejectOutOfRangeStatus() throws Exception {
        adminMockMvc.perform(put("/tsa/admin/community/posts/7/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()));
        Mockito.verify(communityService, Mockito.never())
                .audit(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("PUT /admin/community/posts/{id}/audit - 动态不存在（Service 抛 1002）应回业务码")
    void auditShouldReturn1002WhenPostMissing() throws Exception {
        Mockito.doThrow(new BusinessException(ResultCode.DATA_NOT_FOUND))
                .when(communityService).audit(ArgumentMatchers.eq(999L), ArgumentMatchers.eq(2));

        adminMockMvc.perform(put("/tsa/admin/community/posts/999/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /admin/community/comments - status/postId 应绑定进评论查询对象透传")
    void adminCommentsShouldBindQuery() throws Exception {
        com.tsa.api.dto.CommentVO c = new com.tsa.api.dto.CommentVO();
        c.setId(9L);
        c.setAuthor("老郑");
        c.setContent("潮香居绝了");
        c.setPostId(1L);
        c.setStatus(0);
        Mockito.when(communityService.adminCommentPage(ArgumentMatchers.any()))
                .thenReturn(new PageVO<>(List.of(c), 1L, 1L, 20L));

        adminMockMvc.perform(get("/tsa/admin/community/comments")
                        .param("status", "0").param("postId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value("9"))
                .andExpect(jsonPath("$.data.list[0].postId").value("1"))
                .andExpect(jsonPath("$.data.list[0].status").value(0));

        ArgumentCaptor<com.tsa.api.dto.CommunityCommentAdminQuery> captor =
                ArgumentCaptor.forClass(com.tsa.api.dto.CommunityCommentAdminQuery.class);
        Mockito.verify(communityService).adminCommentPage(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getPostId());
    }

    @Test
    @DisplayName("PUT /admin/community/comments/{id}/audit - 通过透传 1；越界 status 拒之不落 Service")
    void commentAuditShouldPassThroughAndValidate() throws Exception {
        adminMockMvc.perform(put("/tsa/admin/community/comments/9/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Mockito.verify(communityService).auditComment(9L, 1);

        adminMockMvc.perform(put("/tsa/admin/community/comments/9/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.BAD_REQUEST.getCode()));
        Mockito.verify(communityService, Mockito.times(1))
                .auditComment(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt());
    }
}
