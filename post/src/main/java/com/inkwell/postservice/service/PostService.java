package com.inkwell.postservice.service;

import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.entity.PostStatus;

import java.util.List;

public interface PostService {
    PostResponse createPost(PostRequest request, Long userId, String authorizationHeader);
    List<PostResponse> getAllPosts();
    List<PostResponse> getAllPostsForAdmin();
    List<PostResponse> getPostsByStatus(PostStatus status);
    PostResponse getPostById(Long id);
    PostResponse getPostByIdForUser(Long id, Long userId, String role);
    List<PostResponse> getPostsByUserId(Long userId);
    PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId, String role, String authorizationHeader);
    void deletePost(Long postId, Long userId, String role, String authorizationHeader);
    PostResponse updatePostStatus(Long postId, PostStatus status, Long actorId, String authorizationHeader);
}
