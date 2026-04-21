package com.inkwell.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailNotificationRequest {
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be valid")
    private String to;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;
}
