package com.project.appeal.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 申诉记录实体类，对应 appeals 表。
 * <p>
 * 支持三种申诉类型：
 * <ul>
 *   <li>REPUTATION_DEDUCTION — 信誉分扣分申诉</li>
 *   <li>LOW_RATING           — 低评分申诉</li>
 *   <li>PAYMENT_DISPUTE      — 支付争议申诉</li>
 * </ul>
 * 状态流转：PENDING → UNDER_REVIEW → APPROVED / REJECTED
 */
@Getter
@Setter
@Entity
@Table(name = "appeals")
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申诉人用户ID */
    @Column(name = "appellant_id", nullable = false)
    private Long appellantId;

    /**
     * 申诉类型。
     * 枚举值：REPUTATION_DEDUCTION / LOW_RATING / PAYMENT_DISPUTE
     */
    @Column(name = "appeal_type", nullable = false, length = 32)
    private String appealType;

    /**
     * 关联记录ID。
     * REPUTATION_DEDUCTION → reputation_logs.id；
     * LOW_RATING           → reviews.id；
     * PAYMENT_DISPUTE      → payment_records.id
     */
    @Column(name = "target_id")
    private Long targetId;

    /** 申诉理由，不可为空 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /**
     * 证据图片 URL 列表，JSON 格式存储。
     * 示例：["https://cdn.example.com/img1.jpg","https://cdn.example.com/img2.jpg"]
     */
    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls;

    /**
     * 申诉状态。
     * 枚举值：PENDING / UNDER_REVIEW / APPROVED / REJECTED
     */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    /** 审核管理员用户ID（审核后填写） */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 审核意见（审核后填写） */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /** 审核完成时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
