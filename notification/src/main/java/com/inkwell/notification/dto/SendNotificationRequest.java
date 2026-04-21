package com.inkwell.notification.dto;

import com.inkwell.notification.entity.NotificationType;
import com.inkwell.notification.entity.RelatedType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendNotificationRequest {
    @NotNull(message = "Recipient id is required")
    private Long recipientId;

    private Long actorId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    private Long relatedId;

    @NotNull(message = "Related type is required")
    private RelatedType relatedType;

    private boolean sendEmail;
}
