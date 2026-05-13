package com.inkwell.categoryservice.service;

import com.inkwell.categoryservice.dto.CategoryRequest;
import com.inkwell.categoryservice.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryBySlug(String slug);
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getSubCategories(Long parentCategoryId);
    void deleteCategory(Long id);
}
