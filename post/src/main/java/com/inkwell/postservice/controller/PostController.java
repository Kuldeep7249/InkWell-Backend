package com.inkwell.postservice.controller;

import com.inkwell.postservice.dto.ApiResponse;
import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.security.CustomUserDetails;
import com.inkwell.postservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostResponse response = postService.createPost(request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/public")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(postService.getPostsByUserId(userId));
    }
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(postService.updatePost(id, request, userDetails.getUserId()));
    }

    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(id, userDetails.getUserId());
        return ResponseEntity.ok(new ApiResponse("Post deleted successfully"));
    }
}