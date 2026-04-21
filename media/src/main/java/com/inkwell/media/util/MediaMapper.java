package com.inkwell.media.util;

import com.inkwell.media.dto.MediaResponse;
import com.inkwell.media.entity.Media;

public final class MediaMapper {
    private MediaMapper() {}

    public static MediaResponse toResponse(Media media) {
        return MediaResponse.builder()
                .mediaId(media.getMediaId())
                .uploaderId(media.getUploaderId())
                .filename(media.getFilename())
                .originalName(media.getOriginalName())
                .url(media.getUrl())
                .mimeType(media.getMimeType())
                .sizeKb(media.getSizeKb())
                .altText(media.getAltText())
                .linkedPostId(media.getLinkedPostId())
                .uploadedAt(media.getUploadedAt())
                .deleted(media.isDeleted())
                .build();
    }
}
