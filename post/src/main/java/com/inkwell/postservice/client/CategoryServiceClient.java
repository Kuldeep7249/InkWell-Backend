package com.inkwell.postservice.client;

import com.inkwell.postservice.dto.CategoryResponse;
import com.inkwell.postservice.dto.PostCategoryAssignmentRequest;
import com.inkwell.postservice.dto.PostTagAssignmentRequest;
import com.inkwell.postservice.dto.ServiceApiResponse;
import com.inkwell.postservice.dto.TagResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "category-service", url = "${services.category.url:http://localhost:8083}")
public interface CategoryServiceClient {

    @GetMapping("/api/post-categories/{postId}")
    ServiceApiResponse<List<CategoryResponse>> getCategoriesByPost(@PathVariable("postId") Long postId);

    @GetMapping("/api/post-tags/{postId}")
    ServiceApiResponse<List<TagResponse>> getTagsByPost(@PathVariable("postId") Long postId);

    @PostMapping("/api/post-categories")
    void addCategoryToPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                           @RequestBody PostCategoryAssignmentRequest request);

    @DeleteMapping("/api/post-categories")
    void removeCategoryFromPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                @RequestBody PostCategoryAssignmentRequest request);

    @PostMapping("/api/post-tags")
    void addTagToPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                      @RequestBody PostTagAssignmentRequest request);

    @DeleteMapping("/api/post-tags")
    void removeTagFromPost(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                           @RequestBody PostTagAssignmentRequest request);

    @DeleteMapping("/api/posts/{postId}/taxonomy")
    void clearPostTaxonomy(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                           @PathVariable("postId") Long postId);
}
