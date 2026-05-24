package com.project.notification.entity;

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
 * 系统通知实体类，对应 notifications 表。
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 通知标题 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 通知内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 通知类型 */
    @Column(nullable = false, length = 50)
    private String type;

    /** 关联业务类型（experiment/registration/payment/review/appeal） */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    /** 关联业务记录ID */
    @Column(name = "related_id")
    private Long relatedId;

    /** 是否已读 */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** 阅读时间 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
