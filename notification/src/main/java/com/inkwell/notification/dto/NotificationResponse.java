package com.inkwell.notification.dto;

import com.inkwell.notification.entity.NotificationType;
import com.inkwell.notification.entity.RelatedType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String title;
    private String message;
    private Long relatedId;
    private RelatedType relatedType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
