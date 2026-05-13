package com.inkwell.commentservice.service;

import com.inkwell.commentservice.client.PostServiceClient;
import com.inkwell.commentservice.dto.CommentRequest;
import com.inkwell.commentservice.dto.CommentResponse;
import com.inkwell.commentservice.dto.CommentUpdateRequest;
import com.inkwell.commentservice.dto.PostResponse;
import com.inkwell.commentservice.entity.Comment;
import com.inkwell.commentservice.entity.CommentLike;
import com.inkwell.commentservice.entity.CommentStatus;
import com.inkwell.commentservice.exception.BadRequestException;
import com.inkwell.commentservice.exception.ForbiddenOperationException;
import com.inkwell.commentservice.exception.ResourceNotFoundException;
import com.inkwell.commentservice.messaging.NotificationEventPublisher;
import com.inkwell.commentservice.repository.CommentLikeRepository;
import com.inkwell.commentservice.repository.CommentRepository;
import com.inkwell.commentservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private PostServiceClient postServiceClient;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UserPrincipal author;
    private Comment comment;
    private PostResponse post;

    @BeforeEach
    void setUp() {
        author = new UserPrincipal(10L, "author", List.of("AUTHOR"));
        comment = Comment.builder()
                .commentId(1L)
                .postId(100L)
                .authorId(10L)
                .authorUsername("author")
                .content("Original")
                .likesCount(0)
                .status(CommentStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        post = new PostResponse();
        post.setId(100L);
        post.setUserId(20L);
        post.setTitle("Post");
    }

    @Test
    void addCommentAndReplyPersistApprovedCommentsAndNotifyOwners() {
        CommentRequest request = new CommentRequest();
        request.setContent("  Nice post  ");
        when(postServiceClient.getPublicPost(100L)).thenReturn(post);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setCommentId(2L);
            return saved;
        });

        CommentResponse response = commentService.addComment(100L, request, author, "Bearer token");

        assertThat(response.getCommentId()).isEqualTo(2L);
        assertThat(response.getContent()).isEqualTo("Nice post");
        assertThat(response.getStatus()).isEqualTo(CommentStatus.APPROVED);
        verify(notificationEventPublisher).publish(any());

        CommentRequest reply = new CommentRequest();
        reply.setContent("reply");
        reply.setParentCommentId(1L);
        when(commentRepository.findByCommentId(1L)).thenReturn(Optional.of(comment));
        CommentResponse replyResponse = commentService.addComment(100L, reply, new UserPrincipal(30L, "reader", List.of("READER")), "Bearer token");
        assertThat(replyResponse.getParentCommentId()).isEqualTo(1L);
    }

    @Test
    void replyToDifferentPostOrRejectedParentThrows() {
        CommentRequest request = new CommentRequest();
        request.setContent("reply");
        request.setParentCommentId(1L);
        when(postServiceClient.getPublicPost(100L)).thenReturn(post);
        comment.setPostId(200L);
        when(commentRepository.findByCommentId(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.addComment(100L, request, author, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reply must belong to the same post");

        comment.setPostId(100L);
        comment.setStatus(CommentStatus.REJECTED);
        assertThatThrownBy(() -> commentService.addComment(100L, request, author, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot reply to a rejected comment");
    }

    @Test
    void readUpdateDeleteAndModerationPathsEnforceOwnership() {
        when(commentRepository.findByCommentId(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(commentService.getCommentById(1L, false).getContent()).isEqualTo("Original");

        CommentUpdateRequest update = new CommentUpdateRequest();
        update.setContent(" Updated ");
        assertThat(commentService.updateComment(1L, update, author).getContent()).isEqualTo("Updated");

        assertThatThrownBy(() -> commentService.updateComment(1L, update, new UserPrincipal(99L, "other", List.of("AUTHOR"))))
                .isInstanceOf(ForbiddenOperationException.class);

        commentService.deleteComment(1L, new UserPrincipal(99L, "admin", List.of("ADMIN")));
        verify(commentRepository).delete(comment);

        commentService.rejectComment(1L);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.REJECTED);
        commentService.approveComment(1L);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.APPROVED);
    }

    @Test
    void listingRepliesCountsAndLikesUseExpectedRepositories() {
        when(postServiceClient.getPublicPost(100L)).thenReturn(post);
        when(commentRepository.findByPostIdAndStatus(100L, CommentStatus.APPROVED)).thenReturn(List.of(comment));
        assertThat(commentService.getCommentsByPost(100L, false)).hasSize(1);

        when(commentRepository.findByCommentId(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentIdAndStatus(1L, CommentStatus.APPROVED)).thenReturn(List.of(comment));
        assertThat(commentService.getReplies(1L, false)).hasSize(1);

        when(commentRepository.countByPostIdAndStatus(100L, CommentStatus.APPROVED)).thenReturn(3L);
        assertThat(commentService.getCommentCount(100L, false)).isEqualTo(3L);

        when(commentLikeRepository.existsByCommentIdAndUserId(1L, 30L)).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        commentService.likeComment(1L, new UserPrincipal(30L, "reader", List.of("READER")), "Bearer token");
        assertThat(comment.getLikesCount()).isEqualTo(1);

        CommentLike like = CommentLike.builder().commentId(1L).userId(30L).build();
        when(commentLikeRepository.findByCommentIdAndUserId(1L, 30L)).thenReturn(Optional.of(like));
        commentService.unlikeComment(1L, new UserPrincipal(30L, "reader", List.of("READER")));
        assertThat(comment.getLikesCount()).isZero();
        verify(commentLikeRepository).delete(like);
    }

    @Test
    void missingCommentThrowsResourceNotFoundAndDuplicateLikeThrows() {
        when(commentRepository.findByCommentId(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.getCommentById(404L, true))
                .isInstanceOf(ResourceNotFoundException.class);

        when(commentRepository.findByCommentId(1L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(1L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> commentService.likeComment(1L, author, "Bearer token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You already liked this comment");
    }
}
