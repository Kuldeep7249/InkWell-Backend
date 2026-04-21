package com.inkwell.commentservice.service;

import com.inkwell.commentservice.dto.CommentRequest;
import com.inkwell.commentservice.dto.CommentResponse;
import com.inkwell.commentservice.dto.CommentUpdateRequest;
import com.inkwell.commentservice.security.UserPrincipal;

import java.util.List;

public interface CommentService {
    CommentResponse addComment(Long postId, CommentRequest request, UserPrincipal currentUser);
    List<CommentResponse> getCommentsByPost(Long postId, boolean includePendingForModerator);
    CommentResponse getCommentById(Long commentId, boolean includePendingForModerator);
    List<CommentResponse> getReplies(Long commentId, boolean includePendingForModerator);
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request, UserPrincipal currentUser);
    void deleteComment(Long commentId, UserPrincipal currentUser);
    void approveComment(Long commentId);
    void rejectComment(Long commentId);
    void likeComment(Long commentId, UserPrincipal currentUser);
    void unlikeComment(Long commentId, UserPrincipal currentUser);
    long getCommentCount(Long postId, boolean includePendingForModerator);
}
