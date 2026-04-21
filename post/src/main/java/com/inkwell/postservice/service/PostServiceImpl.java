package com.inkwell.postservice.service;

import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.entity.Post;
import com.inkwell.postservice.exception.ResourceNotFoundException;
import com.inkwell.postservice.exception.UnauthorizedActionException;
import com.inkwell.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public PostResponse createPost(PostRequest request, Long userId) {
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mapToResponse(postRepository.save(post));
    }

    @Override
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        return mapToResponse(post);
    }

    @Override
    public List<PostResponse> getPostsByUserId(Long userId) {
        return postRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can update only your own posts");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(postRepository.save(post));
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can delete only your own posts");
        }

        postRepository.delete(post);
    }

    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}