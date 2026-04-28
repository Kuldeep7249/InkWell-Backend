package com.inkwell.notification.service.impl;

import com.inkwell.notification.dto.*;
import com.inkwell.notification.entity.Notification;
import com.inkwell.notification.exception.NotificationDeliveryException;
import com.inkwell.notification.exception.ResourceNotFoundException;
import com.inkwell.notification.exception.UnauthorizedActionException;
import com.inkwell.notification.repository.NotificationRepository;
import com.inkwell.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        if (request.isSendEmail()) {
            log.info("Email notification requested for recipientId={}", request.getRecipientId());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        List<Notification> notifications = request.getRecipientIds().stream()
                .distinct()
                .map(recipientId -> Notification.builder()
                        .recipientId(recipientId)
                        .actorId(request.getActorId())
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .relatedId(request.getRelatedId())
                        .relatedType(request.getRelatedType())
                        .isRead(false)
                        .build())
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);

        if (request.isSendEmail()) {
            log.info("Bulk email notification requested for {} recipients", saved.size());
        }

        return saved.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(Long recipientId, Boolean unreadOnly, Long requesterId, boolean isAdmin) {
        validateAccess(recipientId, requesterId, isAdmin);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Notification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByRecipientIdAndIsRead(recipientId, false, sort)
                : notificationRepository.findByRecipientId(recipientId, sort);
        return notifications.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long requesterId, boolean isAdmin) {
        Notification notification = getOwnedNotification(notificationId, requesterId, isAdmin);
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId, Long requesterId, boolean isAdmin) {
        validateAccess(recipientId, requesterId, isAdmin);
        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsRead(
                recipientId, false, Sort.by(Sort.Direction.DESC, "createdAt"));
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void deleteRead(Long recipientId, Long requesterId, boolean isAdmin) {
        validateAccess(recipientId, requesterId, isAdmin);
        notificationRepository.deleteByRecipientIdAndIsRead(recipientId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long recipientId, Long requesterId, boolean isAdmin) {
        validateAccess(recipientId, requesterId, isAdmin);
        long count = notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
        return UnreadCountResponse.builder()
                .recipientId(recipientId)
                .unreadCount(count)
                .build();
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long requesterId, boolean isAdmin) {
        Notification notification = getOwnedNotification(notificationId, requesterId, isAdmin);
        notificationRepository.delete(notification);
    }

    @Override
    public void sendEmail(EmailNotificationRequest request) {
        validateMailConfiguration();

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailUsername);
        email.setTo(request.getTo());
        email.setSubject(request.getSubject());
        email.setText(request.getBody());
        try {
            mailSender.send(email);
        } catch (MailException ex) {
            throw new NotificationDeliveryException("Email delivery failed: " + resolveMailErrorMessage(ex));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Notification getOwnedNotification(Long notificationId, Long requesterId, boolean isAdmin) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!isAdmin && !notification.getRecipientId().equals(requesterId)) {
            throw new UnauthorizedActionException("You are not allowed to access this notification");
        }
        return notification;
    }

    private void validateAccess(Long recipientId, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !recipientId.equals(requesterId)) {
            throw new UnauthorizedActionException("You can only access your own notifications");
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void validateMailConfiguration() {
        if (mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank()
                || "your_email@gmail.com".equalsIgnoreCase(mailUsername)
                || "your_app_password".equals(mailPassword)) {
            throw new NotificationDeliveryException("Email service is not configured. Set real spring.mail.username and spring.mail.password values.");
        }
    }

    private String resolveMailErrorMessage(MailException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return "Unknown mail server error";
    }
}
