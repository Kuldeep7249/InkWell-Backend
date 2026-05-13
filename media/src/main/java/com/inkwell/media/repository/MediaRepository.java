package com.inkwell.media.repository;

import com.inkwell.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findByMediaIdAndIsDeletedFalse(Long mediaId);
    List<Media> findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(Long uploaderId);
    List<Media> findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtDesc(Long linkedPostId);
    List<Media> findByMimeTypeStartingWithAndIsDeletedFalseOrderByUploadedAtDesc(String mimeTypePrefix);
    List<Media> findByIsDeletedFalseOrderByUploadedAtDesc();
    long countByUploaderIdAndIsDeletedFalse(Long uploaderId);
}
