package com.inkwell.categoryservice.controller;

import com.inkwell.categoryservice.dto.*;
import com.inkwell.categoryservice.service.PostTaxonomyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostTaxonomyController {

    private final PostTaxonomyService postTaxonomyService;

    @Operation(summary = "Assign category to post (AUTHOR/ADMIN)")
    @PostMapping("/api/post-categories")
    public ResponseEntity<ApiResponse<Object>> addCategoryToPost(@Valid @RequestBody PostCategoryAssignmentRequest request) {
        postTaxonomyService.addCategoryToPost(request.getPostId(), request.getCategoryId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Category assigned to post successfully")
                .build());
    }

    @Operation(summary = "Remove category from post (AUTHOR/ADMIN)")
    @DeleteMapping("/api/post-categories")
    public ResponseEntity<ApiResponse<Object>> removeCategoryFromPost(@Valid @RequestBody PostCategoryAssignmentRequest request) {
        postTaxonomyService.removeCategoryFromPost(request.getPostId(), request.getCategoryId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Category removed from post successfully")
                .build());
    }

    @Operation(summary = "Get categories by post")
    @GetMapping("/api/post-categories/{postId}")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Post categories fetched successfully")
                .data(postTaxonomyService.getCategoriesByPost(postId))
                .build());
    }

    @Operation(summary = "Assign tag to post (AUTHOR/ADMIN)")
    @PostMapping("/api/post-tags")
    public ResponseEntity<ApiResponse<Object>> addTagToPost(@Valid @RequestBody PostTagAssignmentRequest request) {
        postTaxonomyService.addTagToPost(request.getPostId(), request.getTagId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Tag assigned to post successfully")
                .build());
    }

    @Operation(summary = "Remove tag from post (AUTHOR/ADMIN)")
    @DeleteMapping("/api/post-tags")
    public ResponseEntity<ApiResponse<Object>> removeTagFromPost(@Valid @RequestBody PostTagAssignmentRequest request) {
        postTaxonomyService.removeTagFromPost(request.getPostId(), request.getTagId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Tag removed from post successfully")
                .build());
    }

    @Operation(summary = "Get tags by post")
    @GetMapping("/api/post-tags/{postId}")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTagsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.<List<TagResponse>>builder()
                .success(true)
                .message("Post tags fetched successfully")
                .data(postTaxonomyService.getTagsByPost(postId))
                .build());
    }

    @Operation(summary = "Remove all taxonomy mappings for a post (AUTHOR/ADMIN)")
    @DeleteMapping("/api/posts/{postId}/taxonomy")
    public ResponseEntity<ApiResponse<Object>> clearPostTaxonomy(@PathVariable Long postId) {
        postTaxonomyService.clearPostTaxonomy(postId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Post taxonomy cleared successfully")
                .build());
    }
}
