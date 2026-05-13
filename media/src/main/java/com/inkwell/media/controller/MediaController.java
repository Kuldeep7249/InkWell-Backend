package com.inkwell.media.controller;

import com.inkwell.media.dto.*;
import com.inkwell.media.security.UserPrincipal;
import com.inkwell.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Upload media")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<MediaResponse>> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "linkedPostId", required = false) Long linkedPostId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal UserPrincipal principal) {

        MediaResponse response = mediaService.uploadMedia(file, altText, linkedPostId, principal.getUserId(), isAdmin(principal), authorizationHeader);
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<MediaResponse>builder()
                .success(true)
                .message("Media uploaded successfully")
                .data(response)
                .build());
    }

    @Operation(summary = "Get media by id")
    @GetMapping("/{mediaId}")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<MediaResponse>> getById(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<MediaResponse>builder()
                .success(true)
                .message("Media fetched successfully")
                .data(mediaService.getMediaById(mediaId, principal.getUserId(), isAdmin(principal)))
                .build());
    }

    @Operation(summary = "Get media by uploader")
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<List<MediaResponse>>> getByUploader(
            @PathVariable Long uploaderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<List<MediaResponse>>builder()
                .success(true)
                .message("Uploader media fetched successfully")
                .data(mediaService.getMediaByUploader(uploaderId, principal.getUserId(), isAdmin(principal)))
                .build());
    }

    @Operation(summary = "Get media linked to post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<List<MediaResponse>>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<List<MediaResponse>>builder()
                .success(true)
                .message("Post media fetched successfully")
                .data(mediaService.getMediaByPost(postId))
                .build());
    }

    @Operation(summary = "Get current user's media, or all media for admin")
    @GetMapping("/all")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<List<MediaResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<List<MediaResponse>>builder()
                .success(true)
                .message("Media fetched successfully")
                .data(mediaService.getAllMedia(principal.getUserId(), isAdmin(principal)))
                .build());
    }

    @Operation(summary = "Admin only: get all media")
    @GetMapping("/admin/all")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<List<MediaResponse>>> getAllForAdmin(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<List<MediaResponse>>builder()
                .success(true)
                .message("All media fetched successfully")
                .data(mediaService.getAllMedia(principal.getUserId(), true))
                .build());
    }

    @Operation(summary = "Update alt text")
    @PutMapping("/{mediaId}/alt-text")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<MediaResponse>> updateAltText(
            @PathVariable Long mediaId,
            @Valid @RequestBody UpdateAltTextRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<MediaResponse>builder()
                .success(true)
                .message("Alt text updated successfully")
                .data(mediaService.updateAltText(mediaId, request.getAltText(), principal.getUserId(), isAdmin(principal)))
                .build());
    }

    @Operation(summary = "Link media to post")
    @PostMapping("/{mediaId}/link")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<MediaResponse>> linkToPost(
            @PathVariable Long mediaId,
            @Valid @RequestBody LinkPostRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<MediaResponse>builder()
                .success(true)
                .message("Media linked to post successfully")
                .data(mediaService.linkToPost(mediaId, request.getPostId(), principal.getUserId(), isAdmin(principal), authorizationHeader))
                .build());
    }

    @Operation(summary = "Unlink media from post")
    @PostMapping("/{mediaId}/unlink")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<MediaResponse>> unlinkFromPost(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<MediaResponse>builder()
                .success(true)
                .message("Media unlinked successfully")
                .data(mediaService.unlinkFromPost(mediaId, principal.getUserId(), isAdmin(principal)))
                .build());
    }

    @Operation(summary = "Soft delete media")
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<Object>> deleteMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.deleteMedia(mediaId, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.builder()
                .success(true)
                .message("Media deleted successfully")
                .data(null)
                .build());
    }

    @Operation(summary = "Admin cleanup of soft deleted files")
    @DeleteMapping("/admin/cleanup")
    public ResponseEntity<com.inkwell.media.dto.ApiResponse<Integer>> cleanupDeleted() {
        return ResponseEntity.ok(com.inkwell.media.dto.ApiResponse.<Integer>builder()
                .success(true)
                .message("Soft-deleted media cleaned successfully")
                .data(mediaService.cleanupDeleted())
                .build());
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
