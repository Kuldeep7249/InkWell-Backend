package com.inkwell.categoryservice.service.impl;

import com.inkwell.categoryservice.dto.TagRequest;
import com.inkwell.categoryservice.dto.TagResponse;
import com.inkwell.categoryservice.entity.Tag;
import com.inkwell.categoryservice.exception.BadRequestException;
import com.inkwell.categoryservice.exception.ResourceNotFoundException;
import com.inkwell.categoryservice.repository.PostTagMappingRepository;
import com.inkwell.categoryservice.repository.TagRepository;
import com.inkwell.categoryservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final PostTagMappingRepository postTagMappingRepository;

    @Override
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Tag with this name already exists");
        }

        Tag tag = Tag.builder()
                .name(request.getName().trim())
                .slug(generateUniqueSlug(request.getName(), null))
                .postCount(0L)
                .build();

        return mapToResponse(tagRepository.save(tag));
    }

    @Override
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

        tagRepository.findByNameIgnoreCase(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Tag with this name already exists");
                });

        tag.setName(request.getName().trim());
        tag.setSlug(generateUniqueSlug(request.getName(), id));

        return mapToResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        return mapToResponse(tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagBySlug(String slug) {
        return mapToResponse(tagRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with slug: " + slug)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getTrendingTags() {
        return tagRepository.findTop10ByOrderByPostCountDescNameAsc().stream().map(this::mapToResponse).toList();
    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

        if (postTagMappingRepository.countByTagId(id) > 0) {
            throw new BadRequestException("Cannot delete tag that is assigned to posts");
        }

        tagRepository.delete(tag);
    }

    private String generateUniqueSlug(String name, Long tagId) {
        String base = slugify(name);
        String candidate = base;
        int counter = 1;

        while (tagRepository.existsBySlug(candidate)) {
            if (tagId != null) {
                Tag existing = tagRepository.findBySlug(candidate).orElse(null);
                if (existing != null && existing.getId().equals(tagId)) {
                    return candidate;
                }
            }
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private TagResponse mapToResponse(Tag tag) {
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
