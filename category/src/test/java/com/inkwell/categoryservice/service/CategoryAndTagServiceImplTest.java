package com.inkwell.categoryservice.service;

import com.inkwell.categoryservice.dto.CategoryRequest;
import com.inkwell.categoryservice.dto.TagRequest;
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
import com.inkwell.categoryservice.service.impl.CategoryServiceImpl;
import com.inkwell.categoryservice.service.impl.PostTaxonomyServiceImpl;
import com.inkwell.categoryservice.service.impl.TagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryAndTagServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PostCategoryMappingRepository postCategoryMappingRepository;
    @Mock
    private PostTagMappingRepository postTagMappingRepository;

    private CategoryServiceImpl categoryService;
    private TagServiceImpl tagService;
    private PostTaxonomyServiceImpl taxonomyService;
    private Category category;
    private Tag tag;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, postCategoryMappingRepository);
        tagService = new TagServiceImpl(tagRepository, postTagMappingRepository);
        taxonomyService = new PostTaxonomyServiceImpl(categoryRepository, tagRepository, postCategoryMappingRepository, postTagMappingRepository);
        category = Category.builder().id(1L).name("Tech").slug("tech").description("All tech").postCount(0L).build();
        tag = Tag.builder().id(2L).name("Java").slug("java").postCount(0L).build();
    }

    @Test
    void categoryCreateUpdateReadDeleteAndDuplicateCases() {
        CategoryRequest request = new CategoryRequest();
        request.setName(" Tech News ");
        request.setDescription("News");
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        assertThat(categoryService.createCategory(request).getSlug()).isEqualTo("tech-news");

        when(categoryRepository.existsByNameIgnoreCase(" Tech News ")).thenReturn(true);
        assertThatThrownBy(() -> categoryService.createCategory(request)).isInstanceOf(BadRequestException.class);

        CategoryRequest update = new CategoryRequest();
        update.setName("Updated");
        update.setDescription("Updated desc");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Updated")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);
        assertThat(categoryService.updateCategory(1L, update).getSlug()).isEqualTo("updated");

        category.setId(1L);
        when(categoryRepository.findBySlug("updated")).thenReturn(Optional.of(category));
        assertThat(categoryService.getCategoryBySlug("updated").getId()).isEqualTo(1L);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        assertThat(categoryService.getAllCategories()).hasSize(1);

        when(categoryRepository.findByParentCategoryId(1L)).thenReturn(List.of());
        when(postCategoryMappingRepository.countByCategoryId(1L)).thenReturn(0L);
        categoryService.deleteCategory(1L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void categoryDeleteRejectsChildrenOrAssignedPosts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentCategoryId(1L)).thenReturn(List.of(Category.builder().id(9L).build()));
        assertThatThrownBy(() -> categoryService.deleteCategory(1L)).isInstanceOf(BadRequestException.class);

        when(categoryRepository.findByParentCategoryId(1L)).thenReturn(List.of());
        when(postCategoryMappingRepository.countByCategoryId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> categoryService.deleteCategory(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void tagCreateUpdateReadTrendingDeleteAndDuplicateCases() {
        TagRequest request = new TagRequest();
        request.setName(" Java ");
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(tagService.createTag(request).getSlug()).isEqualTo("java");

        when(tagRepository.existsByNameIgnoreCase(" Java ")).thenReturn(true);
        assertThatThrownBy(() -> tagService.createTag(request)).isInstanceOf(BadRequestException.class);

        when(tagRepository.findById(2L)).thenReturn(Optional.of(tag));
        when(tagRepository.findByNameIgnoreCase(" Java ")).thenReturn(Optional.empty());
        when(tagRepository.save(tag)).thenReturn(tag);
        assertThat(tagService.updateTag(2L, request).getName()).isEqualTo("Java");

        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(tag));
        assertThat(tagService.getTagBySlug("java").getId()).isEqualTo(2L);
        when(tagRepository.findTop10ByOrderByPostCountDescNameAsc()).thenReturn(List.of(tag));
        assertThat(tagService.getTrendingTags()).hasSize(1);

        when(postTagMappingRepository.countByTagId(2L)).thenReturn(0L);
        tagService.deleteTag(2L);
        verify(tagRepository).delete(tag);
    }

    @Test
    void taxonomyAddRemoveClearAndFetchRefreshPostCounts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postCategoryMappingRepository.findByPostIdAndCategoryId(100L, 1L)).thenReturn(Optional.empty());
        when(postCategoryMappingRepository.countByCategoryId(1L)).thenReturn(1L);
        taxonomyService.addCategoryToPost(100L, 1L);
        assertThat(category.getPostCount()).isEqualTo(1L);

        PostCategoryMapping categoryMapping = PostCategoryMapping.builder().postId(100L).categoryId(1L).build();
        when(postCategoryMappingRepository.findByPostIdAndCategoryId(100L, 1L)).thenReturn(Optional.of(categoryMapping));
        when(postCategoryMappingRepository.countByCategoryId(1L)).thenReturn(0L);
        taxonomyService.removeCategoryFromPost(100L, 1L);
        verify(postCategoryMappingRepository).delete(categoryMapping);

        when(tagRepository.findById(2L)).thenReturn(Optional.of(tag));
        when(postTagMappingRepository.findByPostIdAndTagId(100L, 2L)).thenReturn(Optional.empty());
        when(postTagMappingRepository.countByTagId(2L)).thenReturn(1L);
        taxonomyService.addTagToPost(100L, 2L);
        assertThat(tag.getPostCount()).isEqualTo(1L);

        PostTagMapping tagMapping = PostTagMapping.builder().postId(100L).tagId(2L).build();
        when(postCategoryMappingRepository.findByPostId(100L)).thenReturn(List.of(categoryMapping));
        when(postTagMappingRepository.findByPostId(100L)).thenReturn(List.of(tagMapping));
        taxonomyService.clearPostTaxonomy(100L);
        verify(postCategoryMappingRepository).deleteByPostId(100L);
        verify(postTagMappingRepository).deleteByPostId(100L);
    }

    @Test
    void taxonomyMissingRecordsThrowResourceNotFound() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taxonomyService.addCategoryToPost(100L, 404L))
                .isInstanceOf(ResourceNotFoundException.class);

        when(tagRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taxonomyService.addTagToPost(100L, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
