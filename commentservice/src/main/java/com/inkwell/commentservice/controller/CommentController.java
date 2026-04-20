package com.inkwell.commentservice.controller;

import com.inkwell.commentservice.dto.ApiResponse;
import com.inkwell.commentservice.dto.CommentRequest;
import com.inkwell.commentservice.dto.CommentResponse;
import com.inkwell.commentservice.dto.CommentUpdateRequest;
import com.inkwell.commentservice.security.UserPrincipal;
import com.inkwell.commentservice.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Comment APIs", description = "Operations for comments and replies")
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}")
    @PreAuthorize("hasAnyRole('READER','AUTHOR','ADMIN')")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long postId,
                                                      @Valid @RequestBody CommentRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(postId, request, getPrincipal(authentication)));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<CommentResponse>> getByPost(@PathVariable Long postId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, isModerator(authentication)));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getById(@PathVariable Long commentId,
                                                   Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentById(commentId, isModerator(authentication)));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable Long commentId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(commentService.getReplies(commentId, isModerator(authentication)));
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> update(@PathVariable Long commentId,
                                                  @Valid @RequestBody CommentUpdateRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request, getPrincipal(authentication)));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long commentId,
                                              Authentication authentication) {
        commentService.deleteComment(commentId, getPrincipal(authentication));
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Comment deleted successfully").build());
    }

    @PatchMapping("/{commentId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approve(@PathVariable Long commentId) {
        commentService.approveComment(commentId);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Comment approved successfully").build());
    }

    @PatchMapping("/{commentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> reject(@PathVariable Long commentId) {
        commentService.rejectComment(commentId);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Comment rejected successfully").build());
    }

    @PostMapping("/{commentId}/like")
    @PreAuthorize("hasAnyRole('READER','AUTHOR','ADMIN')")
    public ResponseEntity<ApiResponse> like(@PathVariable Long commentId,
                                            Authentication authentication) {
        commentService.likeComment(commentId, getPrincipal(authentication));
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Comment liked successfully").build());
    }

    @DeleteMapping("/{commentId}/like")
    @PreAuthorize("hasAnyRole('READER','AUTHOR','ADMIN')")
    public ResponseEntity<ApiResponse> unlike(@PathVariable Long commentId,
                                              Authentication authentication) {
        commentService.unlikeComment(commentId, getPrincipal(authentication));
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Comment unliked successfully").build());
    }

    @GetMapping("/posts/{postId}/count")
    public ResponseEntity<Long> count(@PathVariable Long postId,
                                      Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentCount(postId, isModerator(authentication)));
    }

    private UserPrincipal getPrincipal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }

    private boolean isModerator(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.hasRole("ADMIN");
    }
}
