package com.inkwell.media.service;

import com.inkwell.media.client.PostServiceClient;
import com.inkwell.media.dto.MediaResponse;
import com.inkwell.media.entity.Media;
import com.inkwell.media.exception.AccessDeniedException;
import com.inkwell.media.exception.BadRequestException;
import com.inkwell.media.exception.ResourceNotFoundException;
import com.inkwell.media.repository.MediaRepository;
import com.inkwell.media.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PostServiceClient postServiceClient;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;

    @BeforeEach
    void setUp() {
        media = Media.builder()
                .mediaId(1L)
                .uploaderId(10L)
                .filename("generated.png")
                .originalName("original.png")
                .url("http://files/generated.png")
                .mimeType("image/png")
                .sizeKb(1L)
                .altText("alt")
                .linkedPostId(100L)
                .uploadedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }

    @Test
    void uploadValidatesStoresAndSavesMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "abc".getBytes());
        when(fileStorageService.store(any(), anyString())).thenReturn("http://files/generated.png");
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media saved = invocation.getArgument(0);
            saved.setMediaId(99L);
            saved.setUploadedAt(LocalDateTime.now());
            return saved;
        });

        MediaResponse response = mediaService.uploadMedia(file, "Alt text", null, 10L, false, "Bearer token");

        assertThat(response.getMediaId()).isEqualTo(99L);
        assertThat(response.getUploaderId()).isEqualTo(10L);
        assertThat(response.getMimeType()).isEqualTo("image/png");
        verify(fileStorageService).store(any(), anyString());
    }

    @Test
    void uploadRejectsEmptyOversizedAndUnsupportedFiles() {
        assertThatThrownBy(() -> mediaService.uploadMedia(null, null, null, 10L, false, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File is required");

        MockMultipartFile text = new MockMultipartFile("file", "note.txt", "text/plain", "abc".getBytes());
        assertThatThrownBy(() -> mediaService.uploadMedia(text, null, null, 10L, false, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file type");

        byte[] elevenMb = new byte[11 * 1024 * 1024];
        MockMultipartFile large = new MockMultipartFile("file", "large.png", "image/png", elevenMb);
        assertThatThrownBy(() -> mediaService.uploadMedia(large, null, null, 10L, false, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void ownerOrAdminCanReadUpdateLinkUnlinkAndDelete() {
        when(mediaRepository.findByMediaIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(mediaService.getMediaById(1L, 10L, false).getOriginalName()).isEqualTo("original.png");
        assertThat(mediaService.getMediaById(1L, 99L, true).getMediaId()).isEqualTo(1L);
        assertThatThrownBy(() -> mediaService.getMediaById(1L, 99L, false)).isInstanceOf(AccessDeniedException.class);

        assertThat(mediaService.updateAltText(1L, "new alt", 10L, false).getAltText()).isEqualTo("new alt");
        assertThat(mediaService.linkToPost(1L, 200L, 10L, false, "Bearer token").getLinkedPostId()).isEqualTo(200L);
        assertThat(mediaService.unlinkFromPost(1L, 10L, false).getLinkedPostId()).isNull();
        mediaService.deleteMedia(1L, 10L, false);
        assertThat(media.isDeleted()).isTrue();
    }

    @Test
    void listMethodsAndCleanupUseRepositoryAndStorage() {
        when(mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(10L)).thenReturn(List.of(media));
        assertThat(mediaService.getMediaByUploader(10L, 10L, false)).hasSize(1);
        assertThat(mediaService.getAllMedia(10L, false)).hasSize(1);
        assertThatThrownBy(() -> mediaService.getMediaByUploader(10L, 99L, false)).isInstanceOf(AccessDeniedException.class);

        when(mediaRepository.findByIsDeletedFalseOrderByUploadedAtDesc()).thenReturn(List.of(media));
        assertThat(mediaService.getAllMedia(99L, true)).hasSize(1);

        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtDesc(100L)).thenReturn(List.of(media));
        assertThat(mediaService.getMediaByPost(100L)).hasSize(1);

        media.setDeleted(true);
        when(mediaRepository.findAll()).thenReturn(List.of(media));
        assertThat(mediaService.cleanupDeleted()).isEqualTo(1);
        verify(fileStorageService).delete("http://files/generated.png");
        verify(mediaRepository).deleteAll(List.of(media));
    }

    @Test
    void missingMediaThrowsResourceNotFound() {
        when(mediaRepository.findByMediaIdAndIsDeletedFalse(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaService.getMediaById(404L, 10L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Media not found");
    }
}
