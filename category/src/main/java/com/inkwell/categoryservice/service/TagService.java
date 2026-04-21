package com.inkwell.categoryservice.service;

import com.inkwell.categoryservice.dto.TagRequest;
import com.inkwell.categoryservice.dto.TagResponse;

import java.util.List;

public interface TagService {
    TagResponse createTag(TagRequest request);
    TagResponse updateTag(Long id, TagRequest request);
    TagResponse getTagById(Long id);
    TagResponse getTagBySlug(String slug);
    List<TagResponse> getAllTags();
    List<TagResponse> getTrendingTags();
    void deleteTag(Long id);
}
