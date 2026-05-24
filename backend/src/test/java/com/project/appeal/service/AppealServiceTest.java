package com.project.appeal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.entity.Appeal;
import com.project.appeal.repo.AppealRepository;
import com.project.common.exception.ApiException;
import com.project.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * AppealService 单元测试。
 * 使用 Mockito 模拟 AppealRepository 和 NotificationService，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppealService appealService;

    // ===== 测试用辅助数据 =====

    private AppealCreateRequest buildCreateRequest(String type, Long targetId, String reason) {
        AppealCreateRequest req = new AppealCreateRequest();
        req.setAppealType(type);
        req.setTargetId(targetId);
        req.setReason(reason);
        req.setEvidenceUrls("[\"https://example.com/img1.jpg\"]");
        return req;
    }

    private Appeal buildAppeal(Long id, Long appellantId, String type, String status) {
        Appeal a = new Appeal();
        a.setId(id);
        a.setAppellantId(appellantId);
        a.setAppealType(type);
        a.setTargetId(99L);
        a.setReason("测试申诉理由");
        a.setStatus(status);
        a.setCreatedAt(LocalDateTime.now().minusDays(1));
        return a;
    }

    // ===== create() 测试 =====

    @Test
    @DisplayName("create - 正常提交申诉，应返回 PENDING 状态的申诉记录")
    void create_success() {
        // arrange
        AppealCreateRequest req = buildCreateRequest("LOW_RATING", 42L, "评分不公正，请申诉");
        // save() 直接返回传入的对象（模拟 JPA save 行为）
        when(appealRepository.save(any(Appeal.class))).thenAnswer(inv -> {
            Appeal saved = inv.getArgument(0);
            saved.setId(1L); // 模拟数据库生成 ID
            return saved;
        });

        // act
        AppealResponse resp = appealService.create(10L, req);

        // assert
        assertThat(resp.getAppellantId()).isEqualTo(10L);
        assertThat(resp.getAppealType()).isEqualTo("LOW_RATING");
        assertThat(resp.getTargetId()).isEqualTo(42L);
        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getCreatedAt()).isNotNull();

        // 验证 save 被调用一次
        verify(appealRepository, times(1)).save(any(Appeal.class));
    }

    @Test
    @DisplayName("create - 不同申诉类型均可正常创建")
    void create_allTypes() {
        when(appealRepository.save(any(Appeal.class))).thenAnswer(inv -> inv.getArgument(0));

        for (String type : List.of("REPUTATION_DEDUCTION", "LOW_RATING", "PAYMENT_DISPUTE")) {
            AppealCreateRequest req = buildCreateRequest(type, 1L, "理由充分");
            AppealResponse resp = appealService.create(5L, req);
            assertThat(resp.getAppealType()).isEqualTo(type);
            assertThat(resp.getStatus()).isEqualTo("PENDING");
        }
    }

    // ===== getMyAppeals() 测试 =====

    @Test
    @DisplayName("getMyAppeals - 有记录时应返回正确列表")
    void getMyAppeals_withRecords() {
        Appeal a1 = buildAppeal(1L, 10L, "LOW_RATING", "PENDING");
        Appeal a2 = buildAppeal(2L, 10L, "PAYMENT_DISPUTE", "APPROVED");
        when(appealRepository.findByAppellantIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(a1, a2));

        List<AppealResponse> result = appealService.getMyAppeals(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("getMyAppeals - 没有记录时应返回空列表")
    void getMyAppeals_empty() {
        when(appealRepository.findByAppellantIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of());

        List<AppealResponse> result = appealService.getMyAppeals(99L);

        assertThat(result).isEmpty();
    }

    // ===== getAllAppeals() 测试 =====

    @Test
    @DisplayName("getAllAppeals - status 为 null 时查询全部")
    void getAllAppeals_noStatusFilter() {
        Appeal a = buildAppeal(1L, 10L, "LOW_RATING", "PENDING");
        Page<Appeal> page = new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1);
        when(appealRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<AppealResponse> result = appealService.getAllAppeals(null, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(appealRepository).findAllByOrderByCreatedAtDesc(any());
        verify(appealRepository, never()).findByStatusOrderByCreatedAtDesc(anyString(), any());
    }

    @Test
    @DisplayName("getAllAppeals - status 为空字符串时查询全部")
    void getAllAppeals_blankStatusFilter() {
        Page<Appeal> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(appealRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<AppealResponse> result = appealService.getAllAppeals("  ", 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(appealRepository).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getAllAppeals - 有 status 过滤时按状态查询")
    void getAllAppeals_withStatusFilter() {
        Appeal a = buildAppeal(3L, 10L, "PAYMENT_DISPUTE", "PENDING");
        Page<Appeal> page = new PageImpl<>(List.of(a), PageRequest.of(0, 10), 1);
        when(appealRepository.findByStatusOrderByCreatedAtDesc(eq("PENDING"), any()))
                .thenReturn(page);

        Page<AppealResponse> result = appealService.getAllAppeals("PENDING", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
        verify(appealRepository).findByStatusOrderByCreatedAtDesc(eq("PENDING"), any());
        verify(appealRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    // ===== review() 测试 =====

    @Test
    @DisplayName("review - 审核通过：状态变为 APPROVED，发送通知")
    void review_approved() {
        Appeal appeal = buildAppeal(1L, 10L, "LOW_RATING", "PENDING");
        when(appealRepository.findById(1L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(Appeal.class))).thenAnswer(inv -> inv.getArgument(0));

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");
        req.setReviewComment("申诉理由充分");

        AppealResponse resp = appealService.review(1L, 99L, req);

        assertThat(resp.getStatus()).isEqualTo("APPROVED");
        assertThat(resp.getReviewerId()).isEqualTo(99L);
        assertThat(resp.getReviewComment()).isEqualTo("申诉理由充分");
        assertThat(resp.getReviewedAt()).isNotNull();

        // 验证通知被发送
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(
                userIdCaptor.capture(),
                titleCaptor.capture(),
                anyString(),
                eq("APPEAL_PROCESSED"),
                eq("appeal"),
                eq(1L)
        );
        assertThat(userIdCaptor.getValue()).isEqualTo(10L); // 通知发给申诉人
        assertThat(titleCaptor.getValue()).contains("通过");
    }

    @Test
    @DisplayName("review - 审核拒绝：状态变为 REJECTED，发送通知")
    void review_rejected() {
        Appeal appeal = buildAppeal(2L, 20L, "REPUTATION_DEDUCTION", "PENDING");
        when(appealRepository.findById(2L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(Appeal.class))).thenAnswer(inv -> inv.getArgument(0));

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("REJECTED");
        req.setReviewComment("证据不足");

        AppealResponse resp = appealService.review(2L, 99L, req);

        assertThat(resp.getStatus()).isEqualTo("REJECTED");

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(
                eq(20L),
                titleCaptor.capture(),
                anyString(),
                eq("APPEAL_PROCESSED"),
                eq("appeal"),
                eq(2L)
        );
        assertThat(titleCaptor.getValue()).contains("拒绝");
    }

    @Test
    @DisplayName("review - 申诉记录不存在时，抛出 ApiException(404)")
    void review_notFound() {
        when(appealRepository.findById(999L)).thenReturn(Optional.empty());

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");

        assertThatThrownBy(() -> appealService.review(999L, 1L, req))
                .isInstanceOf(ApiException.class)
                .hasMessage("申诉记录不存在")
                .extracting("code").isEqualTo(404);

        verify(appealRepository, never()).save(any());
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("review - 申诉已经是 APPROVED 时，抛出 ApiException(400) 不可重复操作")
    void review_alreadyApproved() {
        Appeal appeal = buildAppeal(3L, 10L, "LOW_RATING", "APPROVED");
        when(appealRepository.findById(3L)).thenReturn(Optional.of(appeal));

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("REJECTED");

        assertThatThrownBy(() -> appealService.review(3L, 1L, req))
                .isInstanceOf(ApiException.class)
                .hasMessage("该申诉已审核完毕，不可重复操作")
                .extracting("code").isEqualTo(400);
    }

    @Test
    @DisplayName("review - 申诉已经是 REJECTED 时，抛出 ApiException(400) 不可重复操作")
    void review_alreadyRejected() {
        Appeal appeal = buildAppeal(4L, 10L, "PAYMENT_DISPUTE", "REJECTED");
        when(appealRepository.findById(4L)).thenReturn(Optional.of(appeal));

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("APPROVED");

        assertThatThrownBy(() -> appealService.review(4L, 1L, req))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(400);
    }

    @Test
    @DisplayName("review - reviewComment 为 null 时通知内容不应包含 null 字符串")
    void review_nullComment_notificationContentOk() {
        Appeal appeal = buildAppeal(5L, 30L, "LOW_RATING", "PENDING");
        when(appealRepository.findById(5L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppealReviewRequest req = new AppealReviewRequest();
        req.setDecision("REJECTED");
        req.setReviewComment(null); // 不填审核意见

        AppealResponse resp = appealService.review(5L, 1L, req);
        assertThat(resp.getStatus()).isEqualTo("REJECTED");

        // 验证通知 content 不含 "null"
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(
                anyLong(), anyString(), contentCaptor.capture(),
                anyString(), anyString(), anyLong()
        );
        assertThat(contentCaptor.getValue()).doesNotContain("null");
    }
}
