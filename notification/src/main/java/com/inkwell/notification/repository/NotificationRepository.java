package com.inkwell.notification.repository;

import com.inkwell.notification.entity.Notification;
import com.inkwell.notification.entity.NotificationType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientId(Long recipientId, Sort sort);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead, Sort sort);
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByRelatedId(Long relatedId);
    void deleteByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
}
