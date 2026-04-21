package com.inkwell.categoryservice.controller;

import com.inkwell.categoryservice.dto.ApiResponse;
import com.inkwell.categoryservice.dto.TagRequest;
import com.inkwell.categoryservice.dto.TagResponse;
import com.inkwell.categoryservice.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "Create tag (ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TagResponse>builder()
                        .success(true)
                        .message("Tag created successfully")
                        .data(tagService.createTag(request))
                        .build());
    }

    @Operation(summary = "Update tag (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(@PathVariable Long id,
                                                              @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(ApiResponse.<TagResponse>builder()
                .success(true)
                .message("Tag updated successfully")
                .data(tagService.updateTag(id, request))
                .build());
    }

    @Operation(summary = "Get all tags")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.<List<TagResponse>>builder()
                .success(true)
                .message("Tags fetched successfully")
                .data(tagService.getAllTags())
                .build());
    }

    @Operation(summary = "Get trending tags")
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTrendingTags() {
        return ResponseEntity.ok(ApiResponse.<List<TagResponse>>builder()
                .success(true)
                .message("Trending tags fetched successfully")
                .data(tagService.getTrendingTags())
                .build());
    }

    @Operation(summary = "Get tag by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TagResponse>builder()
                .success(true)
                .message("Tag fetched successfully")
                .data(tagService.getTagById(id))
                .build());
    }

    @Operation(summary = "Get tag by slug")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.<TagResponse>builder()
                .success(true)
                .message("Tag fetched successfully")
                .data(tagService.getTagBySlug(slug))
                .build());
    }

    @Operation(summary = "Delete tag (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Tag deleted successfully")
                .build());
    }
}
