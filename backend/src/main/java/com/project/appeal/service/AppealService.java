package com.project.appeal.service;

import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.entity.Appeal;
import com.project.appeal.repo.AppealRepository;
import com.project.common.exception.ApiException;
import com.project.notification.service.NotificationService;
import com.project.payment.entity.PaymentRecord;
import com.project.payment.repo.PaymentRecordRepository;
import com.project.registration.RegistrationConstants;
import com.project.registration.entity.Registration;
import com.project.registration.repo.RegistrationRepository;
import com.project.reputation.entity.ReputationLog;
import com.project.reputation.repo.ReputationRepository;
import com.project.review.entity.Review;
import com.project.review.repo.ReviewRepository;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 申诉审核服务层。
 * <p>
 * 业务流程：
 * <ol>
 *   <li>用户提交申诉（PENDING）</li>
 *   <li>管理员审核，状态流转至 APPROVED 或 REJECTED</li>
 *   <li>审核通过后根据申诉类型执行撤销动作，并通知申诉人</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AppealService {

    private final AppealRepository appealRepository;
    private final NotificationService notificationService;
    private final ReputationRepository reputationRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    // ==================== 用户操作 ====================

    /**
     * 用户提交申诉。
     *
     * @param appellantId 申诉人用户ID
     * @param request     申诉请求体
     * @return 创建后的申诉响应体
     */
    @Transactional
    public AppealResponse create(Long appellantId, AppealCreateRequest request) {
        Appeal appeal = new Appeal();
        appeal.setAppellantId(appellantId);
        appeal.setAppealType(request.getAppealType());
        appeal.setTargetId(request.getTargetId());
        appeal.setReason(request.getReason());
        appeal.setEvidenceUrls(request.getEvidenceUrls());
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(LocalDateTime.now());
        appealRepository.save(appeal);
        return AppealResponse.from(appeal);
    }

    /**
     * 查询当前用户提交的所有申诉（按创建时间倒序）。
     *
     * @param appellantId 申诉人用户ID
     * @return 申诉列表
     */
    public List<AppealResponse> getMyAppeals(Long appellantId) {
        return appealRepository.findByAppellantIdOrderByCreatedAtDesc(appellantId)
                .stream()
                .map(AppealResponse::from)
                .collect(Collectors.toList());
    }

    // ==================== 管理员操作 ====================

    /**
     * 管理员分页查询所有申诉，可按状态过滤。
     *
     * @param status 状态过滤（为 null 时查全部）
     * @param page   页码（从0开始）
     * @param size   每页条数
     * @return 分页申诉列表
     */
    public Page<AppealResponse> getAllAppeals(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appeal> appeals = (status == null || status.isBlank())
                ? appealRepository.findAllByOrderByCreatedAtDesc(pageable)
                : appealRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return appeals.map(AppealResponse::from);
    }

    /**
     * 管理员审核申诉。
     * <p>
     * 审核通过（APPROVED）时将申诉状态置为 APPROVED，并向申诉人发送通知；
     * 审核拒绝（REJECTED）时将申诉状态置为 REJECTED，并向申诉人发送通知。
     *
     * @param appealId   申诉记录ID
     * @param reviewerId 审核管理员用户ID
     * @param request    审核请求体（decision + reviewComment）
     * @return 更新后的申诉响应体
     */
    @Transactional
    public AppealResponse review(Long appealId, Long reviewerId, AppealReviewRequest request) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ApiException(404, "申诉记录不存在"));

        if ("APPROVED".equals(appeal.getStatus()) || "REJECTED".equals(appeal.getStatus())) {
            throw new ApiException(400, "该申诉已审核完毕，不可重复操作");
        }

        appeal.setStatus(request.getDecision());
        appeal.setReviewerId(reviewerId);
        appeal.setReviewComment(request.getReviewComment());
        appeal.setReviewedAt(LocalDateTime.now());
        appealRepository.save(appeal);

        // 审核通过时，根据申诉类型执行实际撤销操作
        if ("APPROVED".equals(request.getDecision())) {
            executeRemediation(appeal);
        }

        // 向申诉人发送审核结果通知
        sendReviewNotification(appeal);

        return AppealResponse.from(appeal);
    }

    // ==================== 审核通过后的撤销逻辑 ====================

    /**
     * 根据申诉类型执行对应的撤销操作。
     */
    private void executeRemediation(Appeal appeal) {
        try {
            switch (appeal.getAppealType()) {
                case "REPUTATION_DEDUCTION":
                    reverseReputationDeduction(appeal);
                    break;
                case "LOW_RATING":
                    reverseLowRating(appeal);
                    break;
                case "PAYMENT_DISPUTE":
                    resolvePaymentDispute(appeal);
                    break;
            }
        } catch (Exception e) {
            // 撤销失败不影响申诉状态变更，记录日志即可
            System.err.println("Appeal remediation failed for appeal " + appeal.getId() + ": " + e.getMessage());
        }
    }

    /**
     * 撤销信誉扣分：将扣分记录对应的分数加回用户信誉分。
     */
    private void reverseReputationDeduction(Appeal appeal) {
        reputationRepository.findById(appeal.getTargetId()).ifPresent(log -> {
            // 恢复信誉分
            User user = userRepository.findById(log.getUserId()).orElse(null);
            if (user != null && log.getScoreDelta() < 0) {
                user.setReputationScore(user.getReputationScore() - log.getScoreDelta());
                userRepository.save(user);
            }
            // 恢复报名记录（爽约 → 已通过）
            if (log.getRegistrationId() != null) {
                registrationRepository.findById(log.getRegistrationId()).ifPresent(reg -> {
                    reg.setStatus(RegistrationConstants.STATUS_APPROVED);
                    reg.setUpdatedAt(java.time.LocalDateTime.now());
                    registrationRepository.save(reg);

                    notificationService.send(
                        reg.getUserId(),
                        "爽约记录已撤销",
                        "管理员已通过你的申诉，爽约记录已撤销、信誉分已恢复。报名状态已变回「已通过」，请等待研究者重新处理。",
                        "APPEAL_PROCESSED",
                        "registration",
                        reg.getId()
                    );
                });
            }
        });
    }

    /**
     * 撤销低评分：删除对应的评价记录，重新计算被评价人平均分。
     */
    private void reverseLowRating(Appeal appeal) {
        reviewRepository.findById(appeal.getTargetId()).ifPresent(review -> {
            reviewRepository.delete(review);
            // 重新计算被评价人的平均评分
            User reviewed = userRepository.findById(review.getReviewedId()).orElse(null);
            if (reviewed != null) {
                List<Review> allReviews = reviewRepository.findByReviewedIdOrderByCreatedAtDesc(reviewed.getId());
                if (allReviews.isEmpty()) {
                    reviewed.setResearcherRating(null);
                    reviewed.setTotalReviews(0);
                } else {
                    double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(0);
                    reviewed.setResearcherRating(new java.math.BigDecimal(avg)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
                    reviewed.setTotalReviews(allReviews.size());
                }
                userRepository.save(reviewed);
            }
        });
    }

    /**
     * 解决支付争议：将支付记录状态重置为 PENDING，清除确认信息。
     */
    private void resolvePaymentDispute(Appeal appeal) {
        paymentRecordRepository.findById(appeal.getTargetId()).ifPresent(record -> {
            record.setStatus("PENDING");
            record.setPayerConfirmedAt(null);
            record.setPayeeConfirmedAt(null);
            paymentRecordRepository.save(record);
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 审核完成后向申诉人发送站内通知。
     */
    private void sendReviewNotification(Appeal appeal) {
        boolean approved = "APPROVED".equals(appeal.getStatus());
        String title = approved ? "您的申诉已通过" : "您的申诉已被拒绝";
        String content = approved
                ? "管理员已审核通过您的申诉（类型：" + appeal.getAppealType() + "），原操作已撤销，请查看详情。"
                : "管理员已拒绝您的申诉（类型：" + appeal.getAppealType() + "）。"
                  + (appeal.getReviewComment() != null ? " 审核意见：" + appeal.getReviewComment() : "");
        notificationService.send(
                appeal.getAppellantId(),
                title,
                content,
                "APPEAL_PROCESSED",
                "appeal",
                appeal.getId()
        );
    }
}
