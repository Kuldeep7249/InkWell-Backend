package com.inkwell.postservice.service;

import com.inkwell.postservice.client.CategoryServiceClient;
import com.inkwell.postservice.client.CommentServiceClient;
import com.inkwell.postservice.dto.CategoryResponse;
import com.inkwell.postservice.dto.NotificationType;
import com.inkwell.postservice.dto.PostAnalyticsResponse;
import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.PostCategoryAssignmentRequest;
import com.inkwell.postservice.dto.RelatedType;
import com.inkwell.postservice.dto.PostTagAssignmentRequest;
import com.inkwell.postservice.dto.ServiceApiResponse;
import com.inkwell.postservice.dto.SendNotificationRequest;
import com.inkwell.postservice.dto.TagResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.entity.Post;
import com.inkwell.postservice.entity.PostStatus;
import com.inkwell.postservice.exception.ResourceNotFoundException;
import com.inkwell.postservice.exception.UnauthorizedActionException;
import com.inkwell.postservice.messaging.NotificationEventPublisher;
import feign.FeignException;
import com.inkwell.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryServiceClient categoryServiceClient;
    private final CommentServiceClient commentServiceClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    public PostResponse createPost(PostRequest request, Long userId, String authorizationHeader) {
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getResolvedContent())
                .featuredImageUrl(request.getFeaturedImageUrl())
                .mediaUrls(normalizeStrings(request.getMediaUrls()))
                .mediaIds(normalizeIdsAsList(request.getMediaIds()))
                .userId(userId)
                .status(PostStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Post savedPost = postRepository.save(post);
        syncTaxonomy(savedPost.getId(), request.getResolvedCategoryIds(), resolveTagIds(request.getTagIds(), request.getTagNames()), authorizationHeader);
        return mapToResponse(savedPost, fetchAnalytics(savedPost.getId()));
    }

    @Override
    public List<PostResponse> getAllPosts() {
        return mapPostsToResponses(postRepository.findByStatus(PostStatus.APPROVED));
    }

    @Override
    public List<PostResponse> getAllPostsForAdmin() {
        return mapPostsToResponses(postRepository.findAll());
    }

    @Override
    public List<PostResponse> getPostsByStatus(PostStatus status) {
        return mapPostsToResponses(postRepository.findByStatus(status));
    }

    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (post.getStatus() != PostStatus.APPROVED) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }

        return mapToResponse(post, fetchAnalytics(post.getId()));
    }

    @Override
    public PostResponse getPostByIdForUser(Long id, Long userId, String role) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (!isAdmin(role) && !post.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can view only your own draft posts");
        }

        return mapToResponse(post, fetchAnalytics(post.getId()));
    }

    @Override
    public List<PostResponse> getPostsByUserId(Long userId) {
        return mapPostsToResponses(postRepository.findByUserId(userId));
    }

    @Override
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId, String role, String authorizationHeader) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!isAdmin(role) && !post.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can update only your own posts");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getResolvedContent());
        post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        post.setMediaUrls(normalizeStrings(request.getMediaUrls()));
        post.setMediaIds(normalizeIdsAsList(request.getMediaIds()));
        post.setUpdatedAt(LocalDateTime.now());

        Post updatedPost = postRepository.save(post);
        syncTaxonomy(updatedPost.getId(), request.getResolvedCategoryIds(), resolveTagIds(request.getTagIds(), request.getTagNames()), authorizationHeader);
        return mapToResponse(updatedPost, fetchAnalytics(updatedPost.getId()));
    }

    @Override
    public void deletePost(Long postId, Long userId, String role, String authorizationHeader) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!isAdmin(role) && !post.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can delete only your own posts");
        }

        clearTaxonomy(postId, authorizationHeader);
        postRepository.delete(post);
    }

    @Override
    public PostResponse updatePostStatus(Long postId, PostStatus status, Long actorId, String authorizationHeader) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        post.setStatus(status);
        post.setUpdatedAt(LocalDateTime.now());
        Post savedPost = postRepository.save(post);
        sendPostStatusNotification(savedPost, actorId);
        return mapToResponse(savedPost, fetchAnalytics(savedPost.getId()));
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private List<PostResponse> mapPostsToResponses(List<Post> posts) {
        Map<Long, PostAnalyticsResponse> analyticsByPostId = fetchAnalyticsByPostIds(
                posts.stream().map(Post::getId).toList()
        );

        return posts.stream()
                .map(post -> mapToResponse(post, analyticsByPostId.getOrDefault(post.getId(), emptyAnalytics(post.getId()))))
                .toList();
    }

    private PostResponse mapToResponse(Post post, PostAnalyticsResponse analytics) {
        List<CategoryResponse> categories = fetchCategories(post.getId());
        List<TagResponse> tags = fetchTags(post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .description(post.getContent())
                .featuredImageUrl(post.getFeaturedImageUrl())
                .mediaUrls(copyList(post.getMediaUrls()))
                .mediaIds(copyList(post.getMediaIds()))
                .status(post.getStatus())
                .categories(categories)
                .tags(tags)
                .commentCount(analytics.getCommentCount())
                .commentLikeCount(analytics.getCommentLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private Map<Long, PostAnalyticsResponse> fetchAnalyticsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> normalizedPostIds = postIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalizedPostIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return commentServiceClient.getPostAnalytics(normalizedPostIds).stream()
                    .filter(Objects::nonNull)
                    .filter(analytics -> analytics.getPostId() != null)
                    .collect(Collectors.toMap(
                            PostAnalyticsResponse::getPostId,
                            analytics -> analytics,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
        } catch (FeignException ignored) {
            return Collections.emptyMap();
        }
    }

    private PostAnalyticsResponse fetchAnalytics(Long postId) {
        return fetchAnalyticsByPostIds(List.of(postId)).getOrDefault(postId, emptyAnalytics(postId));
    }

    private PostAnalyticsResponse emptyAnalytics(Long postId) {
        return PostAnalyticsResponse.builder()
                .postId(postId)
                .commentCount(0)
                .commentLikeCount(0)
                .build();
    }

    private void syncTaxonomy(Long postId, List<Long> categoryIds, List<Long> tagIds, String authorizationHeader) {
        syncCategories(postId, categoryIds, authorizationHeader);
        syncTags(postId, tagIds, authorizationHeader);
    }

    private void syncCategories(Long postId, List<Long> requestedCategoryIds, String authorizationHeader) {
        Set<Long> desiredIds = normalizeIds(requestedCategoryIds);
        Set<Long> currentIds = fetchCategories(postId).stream()
                .map(CategoryResponse::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        currentIds.stream()
                .filter(categoryId -> !desiredIds.contains(categoryId))
                .forEach(categoryId -> categoryServiceClient.removeCategoryFromPost(
                        authorizationHeader,
                        new PostCategoryAssignmentRequest(postId, categoryId)
                ));

        desiredIds.stream()
                .filter(categoryId -> !currentIds.contains(categoryId))
                .forEach(categoryId -> categoryServiceClient.addCategoryToPost(
                        authorizationHeader,
                        new PostCategoryAssignmentRequest(postId, categoryId)
                ));
    }

    private void syncTags(Long postId, List<Long> requestedTagIds, String authorizationHeader) {
        Set<Long> desiredIds = normalizeIds(requestedTagIds);
        Set<Long> currentIds = fetchTags(postId).stream()
                .map(TagResponse::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        currentIds.stream()
                .filter(tagId -> !desiredIds.contains(tagId))
                .forEach(tagId -> categoryServiceClient.removeTagFromPost(
                        authorizationHeader,
                        new PostTagAssignmentRequest(postId, tagId)
                ));

        desiredIds.stream()
                .filter(tagId -> !currentIds.contains(tagId))
                .forEach(tagId -> categoryServiceClient.addTagToPost(
                        authorizationHeader,
                        new PostTagAssignmentRequest(postId, tagId)
                ));
    }

    private void clearTaxonomy(Long postId, String authorizationHeader) {
        try {
            categoryServiceClient.clearPostTaxonomy(authorizationHeader, postId);
        } catch (FeignException.NotFound ignored) {
        }
    }

    private List<CategoryResponse> fetchCategories(Long postId) {
        try {
            ServiceApiResponse<List<CategoryResponse>> response = categoryServiceClient.getCategoriesByPost(postId);
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (FeignException ignored) {
            return Collections.emptyList();
        }
    }

    private List<TagResponse> fetchTags(Long postId) {
        try {
            ServiceApiResponse<List<TagResponse>> response = categoryServiceClient.getTagsByPost(postId);
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (FeignException ignored) {
            return Collections.emptyList();
        }
    }

    private Set<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }

        return ids.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> normalizeIdsAsList(List<Long> ids) {
        return new ArrayList<>(normalizeIds(ids));
    }

    private <T> List<T> copyList(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        Set<Long> resolvedIds = normalizeIds(tagIds);
        List<String> normalizedNames = normalizeStrings(tagNames);

        if (normalizedNames.isEmpty()) {
            return new ArrayList<>(resolvedIds);
        }

        Set<String> requestedNames = normalizedNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        fetchAllTags().stream()
                .filter(tag -> tag.getName() != null && requestedNames.contains(tag.getName().trim().toLowerCase()))
                .map(TagResponse::getId)
                .filter(Objects::nonNull)
                .forEach(resolvedIds::add);

        return new ArrayList<>(resolvedIds);
    }

    private List<TagResponse> fetchAllTags() {
        try {
            ServiceApiResponse<List<TagResponse>> response = categoryServiceClient.getAllTags();
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (FeignException ignored) {
            return Collections.emptyList();
        }
    }

    private void sendPostStatusNotification(Post post, Long actorId) {
        String title = post.getStatus() == PostStatus.APPROVED ? "Post approved" : "Post rejected";
        String message = post.getStatus() == PostStatus.APPROVED
                ? "Your post \"" + post.getTitle() + "\" has been approved."
                : "Your post \"" + post.getTitle() + "\" has been rejected.";

        notificationEventPublisher.publish(SendNotificationRequest.builder()
                .recipientId(post.getUserId())
                .actorId(actorId)
                .type(NotificationType.SYSTEM_ALERT)
                .title(title)
                .message(message)
                .relatedId(post.getId())
                .relatedType(RelatedType.POST)
                .sendEmail(true)
                .build());
    }
}
