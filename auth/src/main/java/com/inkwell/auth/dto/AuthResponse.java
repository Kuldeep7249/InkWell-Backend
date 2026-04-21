package com.inkwell.auth.dto;

import com.inkwell.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String email;
    private Role role;
    private String message;
}
