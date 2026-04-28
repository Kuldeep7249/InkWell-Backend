package com.inkwell.postservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagResponse {
    private Long id;
    private String name;
    private String slug;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
