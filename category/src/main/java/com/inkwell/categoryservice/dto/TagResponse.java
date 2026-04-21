package com.inkwell.categoryservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TagResponse {
    private Long id;
    private String name;
    private String slug;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
