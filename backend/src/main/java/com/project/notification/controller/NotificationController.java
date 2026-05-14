package com.project.notification.controller;

import com.project.common.api.Result;
import com.project.notification.dto.NotificationResponse;
import com.project.notification.service.NotificationService;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知模块接口。
 * <p>
 * 提供获取通知列表、未读数量、标记已读、删除通知等功能。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 获取当前用户的通知列表（分页）。
     *
     * @param page 页码，从0开始，默认0
     * @param size 每页条数，默认20
     */
    @GetMapping("/my")
    public Result<Page<NotificationResponse>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(notificationService.getMyNotifications(userId, page, size));
    }

    /**
     * 获取当前用户的未读通知数量。
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 标记单条通知为已读。
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    /**
     * 将当前用户所有未读通知标记为已读。
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }

    /**
     * 删除一条通知。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.delete(id, userId);
        return Result.success(null);
    }

    // ==================== 私有方法 ====================

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getId();
    }
}
