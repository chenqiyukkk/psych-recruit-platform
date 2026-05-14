package com.project.notification.service;

import com.project.common.exception.ApiException;
import com.project.notification.dto.NotificationResponse;
import com.project.notification.entity.Notification;
import com.project.notification.repo.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知服务层。
 * <p>
 * 提供通知的查询、已读标记、删除功能，
 * 以及供其他模块调用的 send 方法用于发送通知。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ==================== 对外发送接口（供其他模块调用） ====================

    /**
     * 发送一条通知给指定用户。
     *
     * @param userId      接收用户ID
     * @param title       通知标题
     * @param content     通知内容
     * @param type        通知类型（如 REGISTRATION_APPROVED）
     * @param relatedType 关联业务类型（如 registration）
     * @param relatedId   关联业务记录ID
     */
    @Transactional
    public void send(Long userId, String title, String content,
                     String type, String relatedType, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * 发送通知（无关联业务的简单通知）。
     */
    @Transactional
    public void send(Long userId, String title, String content, String type) {
        send(userId, title, content, type, null, null);
    }

    // ==================== 查询接口 ====================

    /**
     * 分页获取某用户的通知列表。
     */
    public Page<NotificationResponse> getMyNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::toResponse);
    }

    /**
     * 获取某用户的未读通知数量。
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    // ==================== 操作接口 ====================

    /**
     * 标记单条通知为已读。
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(404, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new ApiException(403, "无权操作此通知");
        }
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * 将某用户所有未读通知标记为已读。
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * 删除一条通知。
     */
    @Transactional
    public void delete(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(404, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new ApiException(403, "无权操作此通知");
        }
        notificationRepository.delete(notification);
    }

    // ==================== 私有方法 ====================

    private NotificationResponse toResponse(Notification entity) {
        NotificationResponse resp = new NotificationResponse();
        resp.setId(entity.getId());
        resp.setTitle(entity.getTitle());
        resp.setContent(entity.getContent());
        resp.setType(entity.getType());
        resp.setRelatedType(entity.getRelatedType());
        resp.setRelatedId(entity.getRelatedId());
        resp.setIsRead(entity.getIsRead());
        resp.setReadAt(entity.getReadAt());
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }
}
