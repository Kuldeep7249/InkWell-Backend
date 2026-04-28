package com.inkwell.auth.dto;

import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.RoleRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleChangeRequestDto {
    private Long requestId;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role currentRole;
    private Role requestedRole;
    private RoleRequestStatus status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
}
