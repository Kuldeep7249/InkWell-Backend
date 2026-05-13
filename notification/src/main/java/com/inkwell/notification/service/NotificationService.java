package com.inkwell.notification.service;

import com.inkwell.notification.dto.*;

import java.util.List;

public interface NotificationService {
    NotificationResponse send(SendNotificationRequest request);
    List<NotificationResponse> sendBulk(BulkNotificationRequest request);
    List<NotificationResponse> getByRecipient(Long recipientId, Boolean unreadOnly, Long requesterId, boolean isAdmin);
    NotificationResponse markAsRead(Long notificationId, Long requesterId, boolean isAdmin);
    void markAllRead(Long recipientId, Long requesterId, boolean isAdmin);
    void deleteRead(Long recipientId, Long requesterId, boolean isAdmin);
    UnreadCountResponse getUnreadCount(Long recipientId, Long requesterId, boolean isAdmin);
    void deleteNotification(Long notificationId, Long requesterId, boolean isAdmin);
    void sendEmail(EmailNotificationRequest request);
    List<NotificationResponse> getAll();
}
