package com.project.appeal.dto;

import com.project.appeal.entity.Appeal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 申诉记录响应体，屏蔽实体类直接暴露。
 */
@Getter
@Setter
public class AppealResponse {

    private Long id;
    private Long appellantId;
    private String appealType;
    private Long targetId;
    private String reason;
    private String evidenceUrls;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    /** 从实体构建响应体 */
    public static AppealResponse from(Appeal appeal) {
        AppealResponse r = new AppealResponse();
        r.setId(appeal.getId());
        r.setAppellantId(appeal.getAppellantId());
        r.setAppealType(appeal.getAppealType());
        r.setTargetId(appeal.getTargetId());
        r.setReason(appeal.getReason());
        r.setEvidenceUrls(appeal.getEvidenceUrls());
        r.setStatus(appeal.getStatus());
        r.setReviewerId(appeal.getReviewerId());
        r.setReviewComment(appeal.getReviewComment());
        r.setReviewedAt(appeal.getReviewedAt());
        r.setCreatedAt(appeal.getCreatedAt());
        return r;
    }
}
