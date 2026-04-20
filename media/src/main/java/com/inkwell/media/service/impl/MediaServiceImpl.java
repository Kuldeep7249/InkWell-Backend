package com.inkwell.media.service.impl;

import com.inkwell.media.dto.MediaResponse;
import com.inkwell.media.entity.Media;
import com.inkwell.media.exception.AccessDeniedException;
import com.inkwell.media.exception.BadRequestException;
import com.inkwell.media.exception.ResourceNotFoundException;
import com.inkwell.media.repository.MediaRepository;
import com.inkwell.media.service.FileStorageService;
import com.inkwell.media.service.MediaService;
import com.inkwell.media.util.MediaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;

    @Override
    public MediaResponse uploadMedia(MultipartFile file, String altText, Long linkedPostId, Long requesterId, boolean isAdmin) {
        validateUpload(file);
        String extension = extractExtension(file.getOriginalFilename());
        String generatedFilename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        String url = fileStorageService.store(file, generatedFilename);

        Media media = mediaRepository.save(Media.builder()
                .uploaderId(requesterId)
                .filename(generatedFilename)
                .originalName(file.getOriginalFilename())
                .url(url)
                .mimeType(file.getContentType())
                .sizeKb(Math.max(1, file.getSize() / 1024))
                .altText(altText)
                .linkedPostId(linkedPostId)
                .isDeleted(false)
                .build());

        return MediaMapper.toResponse(media);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMediaById(Long mediaId, Long requesterId, boolean isAdmin) {
        Media media = getActiveMedia(mediaId);
        authorizeOwnership(media, requesterId, isAdmin);
        return MediaMapper.toResponse(media);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getMediaByUploader(Long uploaderId, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !uploaderId.equals(requesterId)) {
            throw new AccessDeniedException("You can only view your own media library");
        }
        return mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(uploaderId)
                .stream().map(MediaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getMediaByPost(Long postId) {
        return mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtDesc(postId)
                .stream().map(MediaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getAllMedia(Long requesterId, boolean isAdmin) {
        if (!isAdmin) {
            return mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(requesterId)
                    .stream().map(MediaMapper::toResponse).toList();
        }
        return mediaRepository.findByIsDeletedFalseOrderByUploadedAtDesc()
                .stream().map(MediaMapper::toResponse).toList();
    }

    @Override
    public MediaResponse updateAltText(Long mediaId, String altText, Long requesterId, boolean isAdmin) {
        Media media = getActiveMedia(mediaId);
        authorizeOwnership(media, requesterId, isAdmin);
        media.setAltText(altText);
        return MediaMapper.toResponse(mediaRepository.save(media));
    }

    @Override
    public MediaResponse linkToPost(Long mediaId, Long postId, Long requesterId, boolean isAdmin) {
        Media media = getActiveMedia(mediaId);
        authorizeOwnership(media, requesterId, isAdmin);
        media.setLinkedPostId(postId);
        return MediaMapper.toResponse(mediaRepository.save(media));
    }

    @Override
    public MediaResponse unlinkFromPost(Long mediaId, Long requesterId, boolean isAdmin) {
        Media media = getActiveMedia(mediaId);
        authorizeOwnership(media, requesterId, isAdmin);
        media.setLinkedPostId(null);
        return MediaMapper.toResponse(mediaRepository.save(media));
    }

    @Override
    public void deleteMedia(Long mediaId, Long requesterId, boolean isAdmin) {
        Media media = getActiveMedia(mediaId);
        authorizeOwnership(media, requesterId, isAdmin);
        media.setDeleted(true);
        mediaRepository.save(media);
    }

    @Override
    public int cleanupDeleted() {
        List<Media> deletedMedia = mediaRepository.findAll().stream().filter(Media::isDeleted).toList();
        deletedMedia.forEach(media -> fileStorageService.delete(media.getUrl()));
        mediaRepository.deleteAll(deletedMedia);
        return deletedMedia.size();
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("File exceeds maximum allowed size of 10 MB");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Unsupported file type. Allowed: JPEG, PNG, GIF, WebP, PDF");
        }
    }

    private Media getActiveMedia(Long mediaId) {
        return mediaRepository.findByMediaIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
    }

    private void authorizeOwnership(Media media, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !media.getUploaderId().equals(requesterId)) {
            throw new AccessDeniedException("You are not allowed to access this media resource");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
