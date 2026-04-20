package com.inkwell.media.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private Long mediaId;
    private Long uploaderId;
    private String filename;
    private String originalName;
    private String url;
    private String mimeType;
    private Long sizeKb;
    private String altText;
    private Long linkedPostId;
    private LocalDateTime uploadedAt;
    private boolean deleted;
}
