package com.inkwell.commentservice.dto;

import com.inkwell.commentservice.entity.CommentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private Long parentCommentId;
    private String content;
    private Integer likesCount;
    private CommentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
