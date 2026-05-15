package com.inkwell.commentservice.repository;

public interface PostCommentAnalyticsProjection {
    Long getPostId();
    long getCommentCount();
    long getCommentLikeCount();
}
