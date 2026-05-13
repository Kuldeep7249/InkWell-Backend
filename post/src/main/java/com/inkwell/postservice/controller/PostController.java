package com.inkwell.postservice.controller;

import com.inkwell.postservice.dto.ApiResponse;
import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.entity.PostStatus;
import com.inkwell.postservice.security.CustomUserDetails;
import com.inkwell.postservice.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "APIs for managing blog posts")
public class PostController {

    private final PostService postService;

    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @Operation(summary = "Create a post", description = "Creates a new post for the authenticated author")
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostResponse response = postService.createPost(request, userDetails.getUserId(), authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get public posts", description = "Returns all publicly available posts")
    @GetMapping("/public")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @Operation(summary = "Get public post by ID", description = "Returns a single publicly available post")
    @GetMapping("/public/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @Operation(summary = "Get posts by user", description = "Returns posts created by the given user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(postService.getPostsByUserId(userId));
    }

    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @Operation(summary = "Get owned or admin post by ID", description = "Returns a post for its author or an admin, including pending and rejected posts")
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostForUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postService.getPostByIdForUser(id, userDetails.getUserId(), userDetails.getRole()));
    }

    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @Operation(summary = "Update a post", description = "Updates an existing post owned by the authenticated author or managed by an admin")
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postService.updatePost(id, request, userDetails.getUserId(), userDetails.getRole(), authorizationHeader));
    }

    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @Operation(summary = "Delete a post", description = "Deletes an existing post owned by the authenticated author or managed by an admin")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePost(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(id, userDetails.getUserId(), userDetails.getRole(), authorizationHeader);
        return ResponseEntity.ok(new ApiResponse("Post deleted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all posts for admin", description = "Returns posts in every approval state")
    @GetMapping("/admin")
    public ResponseEntity<List<PostResponse>> getAllPostsForAdmin() {
        return ResponseEntity.ok(postService.getAllPostsForAdmin());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get posts by status", description = "Returns posts filtered by approval status")
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<List<PostResponse>> getPostsByStatus(@PathVariable PostStatus status) {
        return ResponseEntity.ok(postService.getPostsByStatus(status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a post", description = "Marks a post as approved and visible publicly")
    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<PostResponse> approvePost(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.updatePostStatus(id, PostStatus.APPROVED, userDetails.getUserId(), authorizationHeader));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a post", description = "Marks a post as rejected")
    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<PostResponse> rejectPost(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.updatePostStatus(id, PostStatus.REJECTED, userDetails.getUserId(), authorizationHeader));
    }
}
