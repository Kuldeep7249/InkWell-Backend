package com.inkwell.categoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostCategoryAssignmentRequest {

    @NotNull(message = "Post id is required")
    @Min(value = 1, message = "Post id must be greater than 0")
    private Long postId;

    @NotNull(message = "Category id is required")
    @Min(value = 1, message = "Category id must be greater than 0")
    private Long categoryId;
}
