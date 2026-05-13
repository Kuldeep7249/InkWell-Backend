package com.inkwell.postservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentCategoryId;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
