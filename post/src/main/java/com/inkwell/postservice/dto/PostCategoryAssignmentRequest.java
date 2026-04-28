package com.inkwell.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCategoryAssignmentRequest {
    private Long postId;
    private Long categoryId;
}
