package com.inkwell.notification.dto;

import com.inkwell.notification.entity.NotificationType;
import com.inkwell.notification.entity.RelatedType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BulkNotificationRequest {
    @NotEmpty(message = "Recipient ids are required")
    private List<Long> recipientIds;

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
