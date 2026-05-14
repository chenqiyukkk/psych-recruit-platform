package com.project.notification.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 通知响应 DTO，返回给前端的通知数据。
 */
@Data
public class NotificationResponse {

    private Long id;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型 */
    private String type;

    /** 关联业务类型 */
    private String relatedType;

    /** 关联业务记录ID */
    private Long relatedId;

    /** 是否已读 */
    private Boolean isRead;

    /** 阅读时间 */
    private LocalDateTime readAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
