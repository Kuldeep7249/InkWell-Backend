package com.inkwell.postservice.dto;

import lombok.Builder;
import lombok.Data;
import com.inkwell.postservice.entity.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String description;
    private String featuredImageUrl;
    private List<String> mediaUrls;
    private List<Long> mediaIds;
    private PostStatus status;
    private List<CategoryResponse> categories;
    private List<TagResponse> tags;
    private long commentCount;
    private long commentLikeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
