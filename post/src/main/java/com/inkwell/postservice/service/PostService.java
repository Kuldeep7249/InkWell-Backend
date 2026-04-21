package com.inkwell.postservice.service;

import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;

import java.util.List;

public interface PostService {
    PostResponse createPost(PostRequest request, Long userId);
    List<PostResponse> getAllPosts();
    PostResponse getPostById(Long id);
    List<PostResponse> getPostsByUserId(Long userId);
    PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId);
    void deletePost(Long postId, Long userId);
}