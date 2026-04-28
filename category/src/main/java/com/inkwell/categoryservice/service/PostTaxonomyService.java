package com.inkwell.categoryservice.service;

import com.inkwell.categoryservice.dto.CategoryResponse;
import com.inkwell.categoryservice.dto.TagResponse;

import java.util.List;

public interface PostTaxonomyService {
    void addCategoryToPost(Long postId, Long categoryId);
    void removeCategoryFromPost(Long postId, Long categoryId);
    List<CategoryResponse> getCategoriesByPost(Long postId);

    void addTagToPost(Long postId, Long tagId);
    void removeTagFromPost(Long postId, Long tagId);
    List<TagResponse> getTagsByPost(Long postId);
    void clearPostTaxonomy(Long postId);
}
