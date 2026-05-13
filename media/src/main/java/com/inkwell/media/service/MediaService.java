package com.inkwell.media.service;

import com.inkwell.media.dto.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {
    MediaResponse uploadMedia(MultipartFile file, String altText, Long linkedPostId, Long requesterId, boolean isAdmin, String authorizationHeader);
    MediaResponse getMediaById(Long mediaId, Long requesterId, boolean isAdmin);
    List<MediaResponse> getMediaByUploader(Long uploaderId, Long requesterId, boolean isAdmin);
    List<MediaResponse> getMediaByPost(Long postId);
    List<MediaResponse> getAllMedia(Long requesterId, boolean isAdmin);
    MediaResponse updateAltText(Long mediaId, String altText, Long requesterId, boolean isAdmin);
    MediaResponse linkToPost(Long mediaId, Long postId, Long requesterId, boolean isAdmin, String authorizationHeader);
    MediaResponse unlinkFromPost(Long mediaId, Long requesterId, boolean isAdmin);
    void deleteMedia(Long mediaId, Long requesterId, boolean isAdmin);
    int cleanupDeleted();
}
