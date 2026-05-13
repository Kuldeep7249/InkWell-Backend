package com.inkwell.postservice.service;

import com.inkwell.postservice.client.CategoryServiceClient;
import com.inkwell.postservice.dto.PostRequest;
import com.inkwell.postservice.dto.PostResponse;
import com.inkwell.postservice.dto.ServiceApiResponse;
import com.inkwell.postservice.dto.TagResponse;
import com.inkwell.postservice.dto.UpdatePostRequest;
import com.inkwell.postservice.entity.Post;
import com.inkwell.postservice.entity.PostStatus;
import com.inkwell.postservice.exception.ResourceNotFoundException;
import com.inkwell.postservice.exception.UnauthorizedActionException;
import com.inkwell.postservice.messaging.NotificationEventPublisher;
import com.inkwell.postservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryServiceClient categoryServiceClient;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private PostServiceImpl postService;

    private Post approvedPost;

    @BeforeEach
    void setUp() {
        approvedPost = Post.builder()
                .id(1L)
                .userId(11L)
                .title("Title")
                .content("Body content")
                .status(PostStatus.APPROVED)
                .mediaIds(List.of(5L))
                .mediaUrls(List.of("https://cdn/image.png"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(categoryServiceClient.getCategoriesByPost(any())).thenReturn(emptyResponse());
        when(categoryServiceClient.getTagsByPost(any())).thenReturn(emptyResponse());
    }

    @Test
    void createPostNormalizesInputSyncsTaxonomyAndStartsPending() {
        PostRequest request = new PostRequest();
        request.setTitle("New Post");
        request.setDescription("Description body");
        request.setCategoryId(2L);
        request.setCategoryIds(List.of(2L, 3L));
        request.setTagIds(Arrays.asList(4L, null, 4L));
        request.setMediaIds(Arrays.asList(8L, null, 8L));
        request.setMediaUrls(List.of(" https://cdn/a.png ", "", "https://cdn/a.png"));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(100L);
            return post;
        });

        PostResponse response = postService.createPost(request, 11L, "Bearer token");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(PostStatus.PENDING);
        assertThat(response.getContent()).isEqualTo("Description body");
        assertThat(response.getMediaIds()).containsExactly(8L);
        assertThat(response.getMediaUrls()).containsExactly("https://cdn/a.png");
        verify(categoryServiceClient, atLeastOnce()).addCategoryToPost(any(), any());
        verify(categoryServiceClient, atLeastOnce()).addTagToPost(any(), any());
    }

    @Test
    void getPublicPostReturnsOnlyApprovedAndRejectsPendingAsNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(approvedPost));
        assertThat(postService.getPostById(1L).getTitle()).isEqualTo("Title");

        approvedPost.setStatus(PostStatus.PENDING);
        assertThatThrownBy(() -> postService.getPostById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void ownerOrAdminCanReadUpdateAndDeleteButOtherUserCannot() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(approvedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(postService.getPostByIdForUser(1L, 11L, "AUTHOR").getId()).isEqualTo(1L);
        assertThat(postService.getPostByIdForUser(1L, 99L, "ADMIN").getId()).isEqualTo(1L);
        assertThatThrownBy(() -> postService.getPostByIdForUser(1L, 99L, "AUTHOR"))
                .isInstanceOf(UnauthorizedActionException.class);

        UpdatePostRequest update = new UpdatePostRequest();
        update.setTitle("Updated");
        update.setContent("Updated content");
        update.setMediaUrls(List.of(" image.png "));
        update.setMediaIds(List.of(9L));
        PostResponse updated = postService.updatePost(1L, update, 11L, "AUTHOR", "Bearer token");
        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getMediaUrls()).containsExactly("image.png");

        assertThatThrownBy(() -> postService.updatePost(1L, update, 99L, "AUTHOR", "Bearer token"))
                .isInstanceOf(UnauthorizedActionException.class);
        assertThatThrownBy(() -> postService.deletePost(1L, 99L, "AUTHOR", "Bearer token"))
                .isInstanceOf(UnauthorizedActionException.class);

        postService.deletePost(1L, 11L, "AUTHOR", "Bearer token");
        verify(postRepository).delete(approvedPost);
    }

    @Test
    void listingMethodsMapRepositoryResults() {
        when(postRepository.findByStatus(PostStatus.APPROVED)).thenReturn(List.of(approvedPost));
        when(postRepository.findByUserId(11L)).thenReturn(List.of(approvedPost));
        when(postRepository.findAll()).thenReturn(List.of(approvedPost));

        assertThat(postService.getAllPosts()).hasSize(1);
        assertThat(postService.getPostsByStatus(PostStatus.APPROVED)).hasSize(1);
        assertThat(postService.getPostsByUserId(11L)).hasSize(1);
        assertThat(postService.getAllPostsForAdmin()).hasSize(1);
    }

    @Test
    void adminStatusUpdatePersistsAndPublishesNotification() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(approvedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.updatePostStatus(1L, PostStatus.REJECTED, 99L, "Bearer admin");

        assertThat(response.getStatus()).isEqualTo(PostStatus.REJECTED);
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PostStatus.REJECTED);
        verify(notificationEventPublisher).publish(any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ServiceApiResponse emptyResponse() {
        ServiceApiResponse response = new ServiceApiResponse();
        response.setData(List.of());
        response.setSuccess(true);
        return response;
    }
}
