package com.inkwell.media.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkPostRequest {
    @NotNull(message = "postId is required")
    private Long postId;
}
