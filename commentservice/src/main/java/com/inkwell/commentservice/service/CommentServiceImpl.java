package com.inkwell.commentservice.service;

import com.inkwell.commentservice.client.PostServiceClient;
import com.inkwell.commentservice.dto.CommentRequest;
import com.inkwell.commentservice.dto.CommentResponse;
import com.inkwell.commentservice.dto.CommentUpdateRequest;
import com.inkwell.commentservice.dto.NotificationType;
import com.inkwell.commentservice.dto.PostResponse;
import com.inkwell.commentservice.dto.RelatedType;
import com.inkwell.commentservice.dto.SendNotificationRequest;
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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostServiceClient postServiceClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    public CommentResponse addComment(Long postId, CommentRequest request, UserPrincipal currentUser, String authorizationHeader) {
        PostResponse post = ensurePublicPostExists(postId);

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = findExistingComment(request.getParentCommentId());
            if (!parent.getPostId().equals(postId)) {
                throw new BadRequestException("Reply must belong to the same post");
            }
            if (parent.getStatus() == CommentStatus.REJECTED) {
                throw new BadRequestException("Cannot reply to a rejected comment");
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(currentUser.getUserId())
                .authorUsername(currentUser.getUsername())
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent().trim())
                .likesCount(0)
                .status(CommentStatus.APPROVED)
                .build();

        Comment savedComment = commentRepository.save(comment);
        sendCommentNotification(savedComment, post, parent, currentUser);
        return mapToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getCommentsByPost(Long postId, boolean includePendingForModerator) {
        ensurePublicPostExists(postId);

        List<Comment> comments = includePendingForModerator
                ? commentRepository.findByPostId(postId)
                : commentRepository.findByPostIdAndStatus(postId, CommentStatus.APPROVED);

        return comments.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CommentResponse getCommentById(Long commentId, boolean includePendingForModerator) {
        Comment comment = findExistingComment(commentId);
        if (!includePendingForModerator && comment.getStatus() != CommentStatus.APPROVED) {
            throw new ResourceNotFoundException("Comment not found");
        }
        return mapToResponse(comment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getReplies(Long commentId, boolean includePendingForModerator) {
        findExistingComment(commentId);
        List<Comment> replies = includePendingForModerator
                ? commentRepository.findByParentCommentId(commentId)
                : commentRepository.findByParentCommentIdAndStatus(commentId, CommentStatus.APPROVED);

        return replies.stream().map(this::mapToResponse).toList();
    }

    @Override
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, UserPrincipal currentUser) {
        Comment comment = findExistingComment(commentId);
        ensureOwnerOrAdmin(comment, currentUser);
        ensureNotRejected(comment);

        comment.setContent(request.getContent().trim());
        return mapToResponse(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(Long commentId, UserPrincipal currentUser) {
        Comment comment = findExistingComment(commentId);
        ensureOwnerOrAdmin(comment, currentUser);
        commentRepository.delete(comment);
    }

    @Override
    public void approveComment(Long commentId) {
        Comment comment = findExistingComment(commentId);
        comment.setStatus(CommentStatus.APPROVED);
        commentRepository.save(comment);
    }

    @Override
    public void rejectComment(Long commentId) {
        Comment comment = findExistingComment(commentId);
        comment.setStatus(CommentStatus.REJECTED);
        commentRepository.save(comment);
    }

    @Override
    public void likeComment(Long commentId, UserPrincipal currentUser, String authorizationHeader) {
        Comment comment = findExistingComment(commentId);
        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new BadRequestException("Only approved comments can be liked");
        }
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, currentUser.getUserId())) {
            throw new BadRequestException("You already liked this comment");
        }

        commentLikeRepository.save(CommentLike.builder()
                .commentId(commentId)
                .userId(currentUser.getUserId())
                .build());

        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
        sendLikeNotification(comment, currentUser);
    }

    @Override
    public void unlikeComment(Long commentId, UserPrincipal currentUser) {
        Comment comment = findExistingComment(commentId);
        CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, currentUser.getUserId())
                .orElseThrow(() -> new BadRequestException("You have not liked this comment yet"));

        commentLikeRepository.delete(like);
        comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    @Override
    public long getCommentCount(Long postId, boolean includePendingForModerator) {
        ensurePublicPostExists(postId);

        return includePendingForModerator
                ? commentRepository.countByPostId(postId)
                : commentRepository.countByPostIdAndStatus(postId, CommentStatus.APPROVED);
    }

    private PostResponse ensurePublicPostExists(Long postId) {
        try {
            return postServiceClient.getPublicPost(postId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }
    }

    private Comment findExistingComment(Long commentId) {
        return commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    private void ensureOwnerOrAdmin(Comment comment, UserPrincipal currentUser) {
        boolean isOwner = comment.getAuthorId().equals(currentUser.getUserId());
        boolean isAdmin = currentUser.hasRole("ADMIN");
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You are not allowed to perform this action on this comment");
        }
    }

    private void ensureNotRejected(Comment comment) {
        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new BadRequestException("Rejected comments cannot be updated");
        }
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .authorUsername(comment.getAuthorUsername())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private void sendCommentNotification(Comment comment, PostResponse post, Comment parent, UserPrincipal currentUser) {
        if (parent != null) {
            if (!parent.getAuthorId().equals(currentUser.getUserId())) {
                sendNotification(SendNotificationRequest.builder()
                        .recipientId(parent.getAuthorId())
                        .actorId(currentUser.getUserId())
                        .type(NotificationType.COMMENT_REPLY)
                        .title("New reply to your comment")
                        .message(currentUser.getUsername() + " replied to your comment.")
                        .relatedId(comment.getCommentId())
                        .relatedType(RelatedType.COMMENT)
                        .sendEmail(false)
                        .build());
            }
            return;
        }

        if (post.getUserId() != null && !post.getUserId().equals(currentUser.getUserId())) {
            sendNotification(SendNotificationRequest.builder()
                    .recipientId(post.getUserId())
                    .actorId(currentUser.getUserId())
                    .type(NotificationType.NEW_COMMENT)
                    .title("New comment on your post")
                    .message(currentUser.getUsername() + " commented on your post.")
                    .relatedId(post.getId())
                    .relatedType(RelatedType.POST)
                    .sendEmail(false)
                    .build());
        }
    }

    private void sendLikeNotification(Comment comment, UserPrincipal currentUser) {
        if (comment.getAuthorId().equals(currentUser.getUserId())) {
            return;
        }

        sendNotification(SendNotificationRequest.builder()
                .recipientId(comment.getAuthorId())
                .actorId(currentUser.getUserId())
                .type(NotificationType.LIKE)
                .title("Your comment got a like")
                .message(currentUser.getUsername() + " liked your comment.")
                .relatedId(comment.getCommentId())
                .relatedType(RelatedType.COMMENT)
                .sendEmail(false)
                .build());
    }

    private void sendNotification(SendNotificationRequest request) {
        notificationEventPublisher.publish(request);
    }
}
