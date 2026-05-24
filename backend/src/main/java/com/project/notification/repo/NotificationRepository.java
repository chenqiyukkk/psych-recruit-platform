package com.project.notification.repo;

import com.project.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 通知数据访问层
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 分页查询某用户的通知，按创建时间倒序 */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 查询某用户所有通知（不分页），按创建时间倒序 */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 统计某用户未读通知数量 */
    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    /** 将某用户所有未读通知标记为已读 */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}
