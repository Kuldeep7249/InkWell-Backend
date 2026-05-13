package com.inkwell.media.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String bio;
    private String avatarUrl;
    private String provider;
    private boolean active;
    private LocalDateTime createdAt;
}
