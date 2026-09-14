package com.tsa.api.config;

import com.tsa.api.common.BusinessException;
import com.tsa.api.common.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthRateLimitInterceptor 直测（不起 Spring 上下文，直接喂 MockHttpServletRequest）。
 *
 * <p>钉死 2026-09-14 社区防灌的两个核心设计：
 * ① 归一化桶——点赞/评论跨帖子 id 共享一个桶，换 id 轮询绕不过限频；
 * ② 双窗口——社区写走小时表、认证走分钟表，各桶互相独立、不同 IP 互相独立。
 */
class AuthRateLimitInterceptorTest {

    /** 阈值取生产口径：登录组 10/5/5 每分，社区四桶 5 每时 */
    private AuthRateLimitInterceptor newInterceptor() {
        return new AuthRateLimitInterceptor(10, 5, 5, 5, 5, 5, 5);
    }

    private void pass(AuthRateLimitInterceptor it, String method, String uri, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(ip);
        it.preHandle(req, new MockHttpServletResponse(), new Object());
    }

    private BusinessException boom(AuthRateLimitInterceptor it, String method, String uri, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(ip);
        BusinessException e = assertThrows(BusinessException.class,
                () -> it.preHandle(req, new MockHttpServletResponse(), new Object()));
        assertEquals(ResultCode.TOO_MANY_REQUESTS, e.getResultCode());
        return e;
    }

    @Test
    @DisplayName("点赞：跨不同帖子 id 共享同桶——第 6 次无论打在哪个帖子都 1306")
    void likeShouldShareBucketAcrossPostIds() {
        AuthRateLimitInterceptor it = newInterceptor();
        String ip = "1.2.3.4";
        pass(it, "POST", "/tsa/community/posts/1/like", ip);
        pass(it, "POST", "/tsa/community/posts/2/like", ip);
        pass(it, "POST", "/tsa/community/posts/3/like", ip);
        pass(it, "POST", "/tsa/community/posts/4/like", ip);
        pass(it, "POST", "/tsa/community/posts/5/like", ip);
        // 换 id 也救不了：桶名归一为 community/like
        boom(it, "POST", "/tsa/community/posts/999/like", ip);
        // 另一 IP 独立计数
        assertDoesNotThrow(() -> pass(it, "POST", "/tsa/community/posts/1/like", "5.6.7.8"));
    }

    @Test
    @DisplayName("评论桶与点赞桶互不相干：各自 5 次额度独立")
    void commentBucketShouldBeIndependentFromLike() {
        AuthRateLimitInterceptor it = newInterceptor();
        String ip = "9.9.9.9";
        for (int i = 1; i <= 5; i++) {
            pass(it, "POST", "/tsa/community/posts/" + i + "/comments", ip);
        }
        // 评论用满后点赞仍有全额（不同桶）
        assertDoesNotThrow(() -> pass(it, "POST", "/tsa/community/posts/1/like", ip));
        boom(it, "POST", "/tsa/community/posts/6/comments", ip);
    }

    @Test
    @DisplayName("发布：精确路径 /tsa/community/posts 归进 community/post 桶")
    void publishShouldHaveOwnBucket() {
        AuthRateLimitInterceptor it = newInterceptor();
        String ip = "8.8.8.8";
        for (int i = 0; i < 5; i++) {
            pass(it, "POST", "/tsa/community/posts", ip);
        }
        boom(it, "POST", "/tsa/community/posts", ip);
    }

    @Test
    @DisplayName("GET 读端点与未圈定路径一律放行（社区列表狂刷不归本闸管）")
    void readsAndUnknownPathsShouldPass() {
        AuthRateLimitInterceptor it = newInterceptor();
        for (int i = 0; i < 50; i++) {
            assertDoesNotThrow(() -> pass(it, "GET", "/tsa/community/posts", "7.7.7.7"));
            assertDoesNotThrow(() -> pass(it, "GET", "/tsa/community/posts/1", "7.7.7.7"));
            assertDoesNotThrow(() -> pass(it, "POST", "/tsa/events", "7.7.7.7"));
        }
    }

    @Test
    @DisplayName("认证组仍是分钟窗口：wechat-login 第 11 次 1306、admin-login 第 6 次 1306")
    void authBucketsShouldKeepMinuteLimits() {
        AuthRateLimitInterceptor it = newInterceptor();
        String ip = "6.6.6.6";
        for (int i = 0; i < 10; i++) {
            pass(it, "POST", "/tsa/auth/wechat-login", ip);
        }
        boom(it, "POST", "/tsa/auth/wechat-login", ip);
        for (int i = 0; i < 5; i++) {
            pass(it, "POST", "/tsa/auth/admin-login", ip);
        }
        boom(it, "POST", "/tsa/auth/admin-login", ip);
    }

    @Test
    @DisplayName("上传桶与发布桶互不相干（传图 5 张不占用发帖额度）")
    void uploadBucketShouldBeIndependentFromPublish() {
        AuthRateLimitInterceptor it = newInterceptor();
        String ip = "5.5.5.5";
        for (int i = 0; i < 5; i++) {
            pass(it, "POST", "/tsa/community/uploads", ip);
        }
        boom(it, "POST", "/tsa/community/uploads", ip);
        // 上传用满不影响发帖
        assertDoesNotThrow(() -> pass(it, "POST", "/tsa/community/posts", ip));
    }
}
