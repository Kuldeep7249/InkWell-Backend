package com.inkwell.auth.dto;

import com.inkwell.auth.entity.AuthProvider;
import com.inkwell.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String bio;
    private String avatarUrl;
    private AuthProvider provider;
    private boolean active;
    private LocalDateTime createdAt;
}
