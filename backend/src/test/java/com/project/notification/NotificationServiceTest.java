package com.project.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.ApiException;
import com.project.notification.dto.NotificationResponse;
import com.project.notification.entity.Notification;
import com.project.notification.repo.NotificationRepository;
import com.project.notification.service.NotificationService;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * NotificationService 单元测试。
 * <p>
 * 使用 Mockito 模拟 Repository，不需要启动 Spring 容器，也不需要连接数据库，运行速度很快。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification();
        sampleNotification.setId(1L);
        sampleNotification.setUserId(100L);
        sampleNotification.setTitle("报名审核通过");
        sampleNotification.setContent("您报名的实验「认知负荷实验」已通过审核，请准时参加。");
        sampleNotification.setType("REGISTRATION_APPROVED");
        sampleNotification.setRelatedType("registration");
        sampleNotification.setRelatedId(200L);
        sampleNotification.setIsRead(false);
        sampleNotification.setCreatedAt(LocalDateTime.now());
    }

    // ==================== send() 测试 ====================

    @Test
    @DisplayName("发送通知 - 成功保存到数据库")
    void send_shouldSaveNotification() {
        // 执行
        notificationService.send(100L, "报名审核通过", "您的报名已通过",
                "REGISTRATION_APPROVED", "registration", 200L);

        // 捕获实际保存的对象，验证字段
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getTitle()).isEqualTo("报名审核通过");
        assertThat(saved.getType()).isEqualTo("REGISTRATION_APPROVED");
        assertThat(saved.getIsRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("发送简单通知（无关联业务）- 成功保存")
    void send_simple_shouldSaveNotification() {
        notificationService.send(100L, "系统通知", "欢迎使用心试通", "SYSTEM");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRelatedType()).isNull();
        assertThat(saved.getRelatedId()).isNull();
    }

    // ==================== getMyNotifications() 测试 ====================

    @Test
    @DisplayName("获取通知列表 - 返回分页结果")
    void getMyNotifications_shouldReturnPage() {
        Page<Notification> fakePage = new PageImpl<>(List.of(sampleNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(fakePage);

        Page<NotificationResponse> result = notificationService.getMyNotifications(100L, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        NotificationResponse resp = result.getContent().get(0);
        assertThat(resp.getTitle()).isEqualTo("报名审核通过");
        assertThat(resp.getIsRead()).isFalse();
        assertThat(resp.getType()).isEqualTo("REGISTRATION_APPROVED");
    }

    // ==================== getUnreadCount() 测试 ====================

    @Test
    @DisplayName("获取未读数量 - 返回正确数值")
    void getUnreadCount_shouldReturnCorrectNumber() {
        when(notificationRepository.countByUserIdAndIsRead(100L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(100L);

        assertThat(count).isEqualTo(5L);
    }

    // ==================== markAsRead() 测试 ====================

    @Test
    @DisplayName("标记已读 - 成功更新状态")
    void markAsRead_shouldSetReadTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        notificationService.markAsRead(1L, 100L);

        assertThat(sampleNotification.getIsRead()).isTrue();
        assertThat(sampleNotification.getReadAt()).isNotNull();
        verify(notificationRepository, times(1)).save(sampleNotification);
    }

    @Test
    @DisplayName("标记已读 - 通知不存在时抛出异常")
    void markAsRead_notFound_shouldThrow() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("标记已读 - 操作他人通知时抛出403异常")
    void markAsRead_wrongUser_shouldThrow403() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        // sampleNotification 属于 userId=100，用 userId=999 来操作
        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    @DisplayName("标记已读 - 已读通知不重复保存")
    void markAsRead_alreadyRead_shouldNotSaveAgain() {
        sampleNotification.setIsRead(true);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        notificationService.markAsRead(1L, 100L);

        // 已经是已读状态，不应再调用 save
        verify(notificationRepository, times(0)).save(any());
    }

    // ==================== markAllAsRead() 测试 ====================

    @Test
    @DisplayName("全部标记已读 - 调用 Repository 批量更新")
    void markAllAsRead_shouldCallRepository() {
        notificationService.markAllAsRead(100L);
        verify(notificationRepository, times(1)).markAllAsRead(100L);
    }

    // ==================== delete() 测试 ====================

    @Test
    @DisplayName("删除通知 - 成功删除")
    void delete_shouldCallDelete() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        notificationService.delete(1L, 100L);

        verify(notificationRepository, times(1)).delete(sampleNotification);
    }

    @Test
    @DisplayName("删除通知 - 通知不存在时抛出异常")
    void delete_notFound_shouldThrow() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete(999L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("删除通知 - 操作他人通知时抛出403异常")
    void delete_wrongUser_shouldThrow403() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        assertThatThrownBy(() -> notificationService.delete(1L, 999L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("无权操作");
    }
}
