package com.inkwell.postservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @Size(min = 5, max = 5000, message = "Content must be between 5 and 5000 characters")
    private String content;

    @Size(min = 5, max = 5000, message = "Description must be between 5 and 5000 characters")
    private String description;

    private Long categoryId;

    private List<Long> categoryIds;

    private List<Long> tagIds;

    private List<String> tagNames;

    @Size(max = 1000, message = "Featured image URL must not exceed 1000 characters")
    private String featuredImageUrl;

    private List<@Size(max = 1000, message = "Media URL must not exceed 1000 characters") String> mediaUrls;

    private List<Long> mediaIds;

    @JsonIgnore
    @AssertTrue(message = "Content or description is required")
    public boolean isContentOrDescriptionPresent() {
        return Stream.of(content, description)
                .anyMatch(value -> value != null && !value.isBlank());
    }

    @JsonIgnore
    public String getResolvedContent() {
        return content != null && !content.isBlank() ? content : description;
    }

    @JsonIgnore
    public List<Long> getResolvedCategoryIds() {
        List<Long> resolvedCategoryIds = new ArrayList<>();
        if (categoryId != null) {
            resolvedCategoryIds.add(categoryId);
        }
        if (categoryIds != null) {
            resolvedCategoryIds.addAll(categoryIds);
        }
        return resolvedCategoryIds;
    }
}
