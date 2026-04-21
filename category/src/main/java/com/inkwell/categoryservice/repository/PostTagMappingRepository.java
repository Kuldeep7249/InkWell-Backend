package com.inkwell.categoryservice.repository;

import com.inkwell.categoryservice.entity.PostTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostTagMappingRepository extends JpaRepository<PostTagMapping, Long> {
    List<PostTagMapping> findByPostId(Long postId);
    List<PostTagMapping> findByTagId(Long tagId);
    Optional<PostTagMapping> findByPostIdAndTagId(Long postId, Long tagId);
    void deleteByPostIdAndTagId(Long postId, Long tagId);
    long countByTagId(Long tagId);
}
