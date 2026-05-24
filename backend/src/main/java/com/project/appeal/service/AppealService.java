package com.project.appeal.service;

import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.entity.Appeal;
import com.project.appeal.repo.AppealRepository;
import com.project.common.exception.ApiException;
import com.project.notification.service.NotificationService;
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

        // 向申诉人发送审核结果通知
        sendReviewNotification(appeal);

        return AppealResponse.from(appeal);
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
