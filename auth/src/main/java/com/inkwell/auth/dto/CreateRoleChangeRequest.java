package com.inkwell.auth.dto;

import com.inkwell.auth.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoleChangeRequest {
    @NotNull
    private Role requestedRole;

    @Size(max = 500)
    private String reason;
}
