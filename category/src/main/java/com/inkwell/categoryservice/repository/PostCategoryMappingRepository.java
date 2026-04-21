package com.inkwell.categoryservice.repository;

import com.inkwell.categoryservice.entity.PostCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostCategoryMappingRepository extends JpaRepository<PostCategoryMapping, Long> {
    List<PostCategoryMapping> findByPostId(Long postId);
    List<PostCategoryMapping> findByCategoryId(Long categoryId);
    Optional<PostCategoryMapping> findByPostIdAndCategoryId(Long postId, Long categoryId);
    void deleteByPostIdAndCategoryId(Long postId, Long categoryId);
    long countByCategoryId(Long categoryId);
}
