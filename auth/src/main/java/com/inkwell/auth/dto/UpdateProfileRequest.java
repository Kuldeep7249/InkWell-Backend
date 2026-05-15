package com.inkwell.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    private String fullName;

    @Size(max = 500)
    private String bio;

    @Size(max = 2048)
    private String avatarUrl;
}
