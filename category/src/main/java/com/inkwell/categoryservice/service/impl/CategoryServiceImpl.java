package com.inkwell.categoryservice.service.impl;

import com.inkwell.categoryservice.dto.CategoryRequest;
import com.inkwell.categoryservice.dto.CategoryResponse;
import com.inkwell.categoryservice.entity.Category;
import com.inkwell.categoryservice.exception.BadRequestException;
import com.inkwell.categoryservice.exception.ResourceNotFoundException;
import com.inkwell.categoryservice.repository.CategoryRepository;
import com.inkwell.categoryservice.repository.PostCategoryMappingRepository;
import com.inkwell.categoryservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostCategoryMappingRepository postCategoryMappingRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        validateParentCategory(request.getParentCategoryId(), null);

        String slug = generateUniqueSlug(request.getName(), null);
        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .parentCategoryId(request.getParentCategoryId())
                .postCount(0L)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        categoryRepository.findByNameIgnoreCase(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Category with this name already exists");
                });

        validateParentCategory(request.getParentCategoryId(), id);

        category.setName(request.getName().trim());
        category.setSlug(generateUniqueSlug(request.getName(), id));
        category.setDescription(request.getDescription());
        category.setParentCategoryId(request.getParentCategoryId());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        return mapToResponse(categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubCategories(Long parentCategoryId) {
        return categoryRepository.findByParentCategoryId(parentCategoryId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!categoryRepository.findByParentCategoryId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete category with child categories");
        }

        if (postCategoryMappingRepository.countByCategoryId(id) > 0) {
            throw new BadRequestException("Cannot delete category that is assigned to posts");
        }

        categoryRepository.delete(category);
    }

    private void validateParentCategory(Long parentCategoryId, Long currentCategoryId) {
        if (parentCategoryId == null) {
            return;
        }

        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));

        if (currentCategoryId != null && parent.getId().equals(currentCategoryId)) {
            throw new BadRequestException("Category cannot be its own parent");
        }
    }

    private String generateUniqueSlug(String name, Long categoryId) {
        String base = slugify(name);
        String candidate = base;
        int counter = 1;

        while (categoryRepository.existsBySlug(candidate)) {
            if (categoryId != null) {
                Category existing = categoryRepository.findBySlug(candidate).orElse(null);
                if (existing != null && existing.getId().equals(categoryId)) {
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

    private CategoryResponse mapToResponse(Category category) {
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
}
