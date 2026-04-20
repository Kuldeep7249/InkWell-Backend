package com.inkwell.media.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAltTextRequest {
    @Size(max = 300, message = "Alt text must not exceed 300 characters")
    private String altText;
}
