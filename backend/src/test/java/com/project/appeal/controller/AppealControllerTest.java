package com.project.appeal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.service.AppealService;
import com.project.common.exception.ApiException;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AppealController MockMvc 测试。
 * <p>
 * 使用 @WebMvcTest 只加载 Web 层；AppealService、UserRepository 均 Mock；
 * 引入 GlobalExceptionHandler 以验证异常情况下的响应格式。
 */
@WebMvcTest(AppealController.class)
@Import(GlobalExceptionHandler.class)
class AppealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppealService appealService;

    @MockBean
    private UserRepository userRepository;

    // ===== 公共 stub：模拟从 Authentication 获取用户ID =====

    private static final Long CURRENT_USER_ID = 10L;
    private static final String CURRENT_USERNAME = "testuser";

    @BeforeEach
    void stubCurrentUser() {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        user.setUsername(CURRENT_USERNAME);
        when(userRepository.findByUsername(CURRENT_USERNAME)).thenReturn(Optional.of(user));
    }

    // ===== 辅助方法：构建响应体 =====

    private AppealResponse buildResponse(Long id, String type, String status) {
        AppealResponse r = new AppealResponse();
        r.setId(id);
        r.setAppellantId(CURRENT_USER_ID);
        r.setAppealType(type);
        r.setTargetId(99L);
        r.setReason("测试申诉理由");
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ===== POST /api/appeals =====

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("POST /api/appeals - 正常提交申诉，返回 200 和 PENDING 记录")
    void createAppeal_success() throws Exception {
        AppealCreateRequest req = new AppealCreateRequest();
        req.setAppealType("LOW_RATING");
        req.setTargetId(42L);
        req.setReason("评分不公正，申请复议");

        AppealResponse resp = buildResponse(1L, "LOW_RATING", "PENDING");
        when(appealService.create(eq(CURRENT_USER_ID), any())).thenReturn(resp);

        mockMvc.perform(post("/api/appeals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.appealType").value("LOW_RATING"))
                .andExpect(jsonPath("$.data.appellantId").value(CURRENT_USER_ID));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("POST /api/appeals - appealType 不合法时，返回 400")
    void createAppeal_invalidType() throws Exception {
        AppealCreateRequest req = new AppealCreateRequest();
        req.setAppealType("INVALID_TYPE"); // 不在枚举值内
        req.setTargetId(1L);
        req.setReason("理由足够长度");

        mockMvc.perform(post("/api/appeals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("POST /api/appeals - reason 为空时，返回 400 参数校验失败")
    void createAppeal_blankReason() throws Exception {
        AppealCreateRequest req = new AppealCreateRequest();
        req.setAppealType("LOW_RATING");
        req.setTargetId(1L);
        req.setReason(""); // 空理由

        mockMvc.perform(post("/api/appeals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("POST /api/appeals - targetId 为 null 时，返回 400 参数校验失败")
    void createAppeal_nullTargetId() throws Exception {
        AppealCreateRequest req = new AppealCreateRequest();
        req.setAppealType("PAYMENT_DISPUTE");
        req.setTargetId(null); // 缺少 targetId
        req.setReason("支付有争议");

        mockMvc.perform(post("/api/appeals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ===== GET /api/appeals/my =====

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("GET /api/appeals/my - 返回当前用户的申诉列表")
    void getMyAppeals_success() throws Exception {
        List<AppealResponse> list = List.of(
                buildResponse(1L, "LOW_RATING", "PENDING"),
                buildResponse(2L, "PAYMENT_DISPUTE", "APPROVED")
        );
        when(appealService.getMyAppeals(CURRENT_USER_ID)).thenReturn(list);

        mockMvc.perform(get("/api/appeals/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("GET /api/appeals/my - 无记录时返回空数组")
    void getMyAppeals_empty() throws Exception {
        when(appealService.getMyAppeals(CURRENT_USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/appeals/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ===== GET /api/appeals =====

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("GET /api/appeals - 不带 status 参数时查询全部，返回分页结果")
    void getAllAppeals_noFilter() throws Exception {
        AppealResponse r = buildResponse(1L, "LOW_RATING", "PENDING");
        Page<AppealResponse> page = new PageImpl<>(List.of(r), PageRequest.of(0, 20), 1);
        when(appealService.getAllAppeals(isNull(), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/appeals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("GET /api/appeals - 带 status=PENDING 过滤时仅返回对应记录")
    void getAllAppeals_withStatusFilter() throws Exception {
        AppealResponse r = buildResponse(2L, "PAYMENT_DISPUTE", "PENDING");
        Page<AppealResponse> page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(appealService.getAllAppeals(eq("PENDING"), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/appeals")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    // ===== PUT /api/appeals/{id}/review =====

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("PUT /api/appeals/{id}/review - 审核通过，返回 APPROVED 记录")
    void reviewAppeal_approved() throws Exception {
        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");
        req.setReviewComment("申诉合理");

        AppealResponse resp = buildResponse(1L, "LOW_RATING", "APPROVED");
        resp.setReviewerId(CURRENT_USER_ID);
        resp.setReviewComment("申诉合理");
        when(appealService.review(eq(1L), eq(CURRENT_USER_ID), any())).thenReturn(resp);

        mockMvc.perform(put("/api/appeals/1/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewComment").value("申诉合理"));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("PUT /api/appeals/{id}/review - 审核拒绝，返回 REJECTED 记录")
    void reviewAppeal_rejected() throws Exception {
        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("REJECTED");
        req.setReviewComment("证据不足");

        AppealResponse resp = buildResponse(1L, "LOW_RATING", "REJECTED");
        when(appealService.review(eq(1L), eq(CURRENT_USER_ID), any())).thenReturn(resp);

        mockMvc.perform(put("/api/appeals/1/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("PUT /api/appeals/{id}/review - decision 为非法值时，返回 400")
    void reviewAppeal_invalidDecision() throws Exception {
        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("MAYBE"); // 非法值

        mockMvc.perform(put("/api/appeals/1/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("PUT /api/appeals/{id}/review - 申诉记录不存在时，Service 抛出 ApiException(404)，返回 400")
    void reviewAppeal_notFound() throws Exception {
        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");

        when(appealService.review(eq(999L), anyLong(), any()))
                .thenThrow(new ApiException(404, "申诉记录不存在"));

        mockMvc.perform(put("/api/appeals/999/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("申诉记录不存在"));
    }

    @Test
    @WithMockUser(username = CURRENT_USERNAME)
    @DisplayName("PUT /api/appeals/{id}/review - 重复审核时，Service 抛出 ApiException(400)，返回 400")
    void reviewAppeal_alreadyReviewed() throws Exception {
        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");

        when(appealService.review(eq(1L), anyLong(), any()))
                .thenThrow(new ApiException(400, "该申诉已审核完毕，不可重复操作"));

        mockMvc.perform(put("/api/appeals/1/review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该申诉已审核完毕，不可重复操作"));
    }
}
