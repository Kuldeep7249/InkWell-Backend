package com.inkwell.notification.service;

import com.inkwell.notification.client.AuthServiceClient;
import com.inkwell.notification.dto.AuthProfileResponse;
import com.inkwell.notification.dto.BulkNotificationRequest;
import com.inkwell.notification.dto.EmailNotificationRequest;
import com.inkwell.notification.dto.NotificationResponse;
import com.inkwell.notification.dto.SendNotificationRequest;
import com.inkwell.notification.entity.Notification;
import com.inkwell.notification.entity.NotificationType;
import com.inkwell.notification.entity.RelatedType;
import com.inkwell.notification.exception.NotificationDeliveryException;
import com.inkwell.notification.exception.ResourceNotFoundException;
import com.inkwell.notification.exception.UnauthorizedActionException;
import com.inkwell.notification.repository.NotificationRepository;
import com.inkwell.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .notificationId(1L)
                .recipientId(10L)
                .actorId(20L)
                .type(NotificationType.NEW_COMMENT)
                .title("Title")
                .message("Message")
                .relatedId(100L)
                .relatedType(RelatedType.POST)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void sendAndSendBulkPersistNotifications() {
        SendNotificationRequest request = singleRequest();
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.send(request);

        assertThat(response.getNotificationId()).isEqualTo(1L);
        assertThat(response.getRecipientId()).isEqualTo(10L);

        BulkNotificationRequest bulk = new BulkNotificationRequest();
        bulk.setRecipientIds(List.of(10L, 10L, 11L));
        bulk.setActorId(20L);
        bulk.setType(NotificationType.NEW_COMMENT);
        bulk.setTitle("Bulk");
        bulk.setMessage("Message");
        bulk.setRelatedId(100L);
        bulk.setRelatedType(RelatedType.POST);
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(notificationService.sendBulk(bulk)).hasSize(2);
    }

    @Test
    void recipientReadsMarksDeletesAndCountsOwnNotifications() {
        when(notificationRepository.findByRecipientId(10L, Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(notification));
        assertThat(notificationService.getByRecipient(10L, false, 10L, false)).hasSize(1);

        when(notificationRepository.findByRecipientIdAndIsRead(10L, false, Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(notification));
        assertThat(notificationService.getByRecipient(10L, true, 10L, false)).hasSize(1);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        assertThat(notificationService.markAsRead(1L, 10L, false).getIsRead()).isTrue();

        notification.setIsRead(false);
        when(notificationRepository.saveAll(List.of(notification))).thenReturn(List.of(notification));
        notificationService.markAllRead(10L, 10L, false);
        assertThat(notification.getIsRead()).isTrue();

        when(notificationRepository.countByRecipientIdAndIsRead(10L, false)).thenReturn(4L);
        assertThat(notificationService.getUnreadCount(10L, 10L, false).getUnreadCount()).isEqualTo(4L);

        notificationService.deleteRead(10L, 10L, false);
        verify(notificationRepository).deleteByRecipientIdAndIsRead(10L, true);

        notificationService.deleteNotification(1L, 10L, false);
        verify(notificationRepository).delete(notification);
    }

    @Test
    void unauthorizedAndMissingNotificationCasesThrow() {
        assertThatThrownBy(() -> notificationService.getByRecipient(10L, false, 99L, false))
                .isInstanceOf(UnauthorizedActionException.class);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L, false))
                .isInstanceOf(UnauthorizedActionException.class);

        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markAsRead(404L, 10L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendEmailRequiresConfigurationAndWrapsMailFailures() {
        EmailNotificationRequest request = new EmailNotificationRequest();
        request.setTo("reader@example.com");
        request.setSubject("Subject");
        request.setBody("Body");

        assertThatThrownBy(() -> notificationService.sendEmail(request))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("Email service is not configured");

        ReflectionTestUtils.setField(notificationService, "mailUsername", "sender@example.com");
        ReflectionTestUtils.setField(notificationService, "mailPassword", "app-password");
        notificationService.sendEmail(request);
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        org.mockito.Mockito.doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        assertThatThrownBy(() -> notificationService.sendEmail(request))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("Email delivery failed");
    }

    @Test
    void sendNotificationWithEmailResolvesRecipientAndSendsMail() {
        SendNotificationRequest request = singleRequest();
        request.setSendEmail(true);

        AuthProfileResponse profile = new AuthProfileResponse();
        profile.setUserId(10L);
        profile.setEmail("reader@example.com");

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(authServiceClient.getUserById(10L)).thenReturn(profile);
        ReflectionTestUtils.setField(notificationService, "mailUsername", "sender@example.com");
        ReflectionTestUtils.setField(notificationService, "mailPassword", "app-password");

        notificationService.send(request);

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void adminCanReadAllNotifications() {
        when(notificationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(notification));
        assertThat(notificationService.getAll()).hasSize(1);
        assertThat(notificationService.getByRecipient(10L, false, 99L, true)).isEmpty();
    }

    private SendNotificationRequest singleRequest() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientId(10L);
        request.setActorId(20L);
        request.setType(NotificationType.NEW_COMMENT);
        request.setTitle("Title");
        request.setMessage("Message");
        request.setRelatedId(100L);
        request.setRelatedType(RelatedType.POST);
        return request;
    }
}
