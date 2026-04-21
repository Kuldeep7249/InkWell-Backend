package com.inkwell.categoryservice.service.impl;

import com.inkwell.categoryservice.dto.CategoryResponse;
import com.inkwell.categoryservice.dto.TagResponse;
import com.inkwell.categoryservice.entity.Category;
import com.inkwell.categoryservice.entity.PostCategoryMapping;
import com.inkwell.categoryservice.entity.PostTagMapping;
import com.inkwell.categoryservice.entity.Tag;
import com.inkwell.categoryservice.exception.BadRequestException;
import com.inkwell.categoryservice.exception.ResourceNotFoundException;
import com.inkwell.categoryservice.repository.CategoryRepository;
import com.inkwell.categoryservice.repository.PostCategoryMappingRepository;
import com.inkwell.categoryservice.repository.PostTagMappingRepository;
import com.inkwell.categoryservice.repository.TagRepository;
import com.inkwell.categoryservice.service.PostTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostTaxonomyServiceImpl implements PostTaxonomyService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostCategoryMappingRepository postCategoryMappingRepository;
    private final PostTagMappingRepository postTagMappingRepository;

    @Override
    public void addCategoryToPost(Long postId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (postCategoryMappingRepository.findByPostIdAndCategoryId(postId, categoryId).isPresent()) {
            throw new BadRequestException("Category already assigned to this post");
        }

        postCategoryMappingRepository.save(PostCategoryMapping.builder()
                .postId(postId)
                .categoryId(categoryId)
                .build());

        category.setPostCount(postCategoryMappingRepository.countByCategoryId(categoryId));
        categoryRepository.save(category);
    }

    @Override
    public void removeCategoryFromPost(Long postId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        PostCategoryMapping mapping = postCategoryMappingRepository.findByPostIdAndCategoryId(postId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category mapping not found for post"));

        postCategoryMappingRepository.delete(mapping);
        category.setPostCount(postCategoryMappingRepository.countByCategoryId(categoryId));
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByPost(Long postId) {
        return postCategoryMappingRepository.findByPostId(postId).stream()
                .map(mapping -> categoryRepository.findById(mapping.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + mapping.getCategoryId())))
                .map(this::mapCategory)
                .toList();
    }

    @Override
    public void addTagToPost(Long postId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (postTagMappingRepository.findByPostIdAndTagId(postId, tagId).isPresent()) {
            throw new BadRequestException("Tag already assigned to this post");
        }

        postTagMappingRepository.save(PostTagMapping.builder()
                .postId(postId)
                .tagId(tagId)
                .build());

        tag.setPostCount(postTagMappingRepository.countByTagId(tagId));
        tagRepository.save(tag);
    }

    @Override
    public void removeTagFromPost(Long postId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        PostTagMapping mapping = postTagMappingRepository.findByPostIdAndTagId(postId, tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag mapping not found for post"));

        postTagMappingRepository.delete(mapping);
        tag.setPostCount(postTagMappingRepository.countByTagId(tagId));
        tagRepository.save(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getTagsByPost(Long postId) {
        return postTagMappingRepository.findByPostId(postId).stream()
                .map(mapping -> tagRepository.findById(mapping.getTagId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + mapping.getTagId())))
                .map(this::mapTag)
                .toList();
    }

    private CategoryResponse mapCategory(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategoryId())
                .postCount(category.getPostCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private TagResponse mapTag(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount(tag.getPostCount())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
