package com.inkwell.categoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostTagAssignmentRequest {

    @NotNull(message = "Post id is required")
    @Min(value = 1, message = "Post id must be greater than 0")
    private Long postId;

    @NotNull(message = "Tag id is required")
    @Min(value = 1, message = "Tag id must be greater than 0")
    private Long tagId;
}
