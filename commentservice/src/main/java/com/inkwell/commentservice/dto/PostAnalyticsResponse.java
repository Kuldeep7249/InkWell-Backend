package com.inkwell.commentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAnalyticsResponse {
    private Long postId;
    private long commentCount;
    private long commentLikeCount;
}
