package com.inkwell.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginOtpChallengeResponse {
    private Long challengeId;
    private String email;
    private String maskedEmail;
    private LocalDateTime expiresAt;
    private String message;
}
