package com.inkwell.commentservice.service;

import com.inkwell.commentservice.dto.CommentRequest;
import com.inkwell.commentservice.dto.CommentResponse;
import com.inkwell.commentservice.dto.CommentUpdateRequest;
import com.inkwell.commentservice.entity.Comment;
import com.inkwell.commentservice.entity.CommentLike;
import com.inkwell.commentservice.entity.CommentStatus;
import com.inkwell.commentservice.exception.BadRequestException;
import com.inkwell.commentservice.exception.ForbiddenOperationException;
import com.inkwell.commentservice.exception.ResourceNotFoundException;
import com.inkwell.commentservice.repository.CommentLikeRepository;
import com.inkwell.commentservice.repository.CommentRepository;
import com.inkwell.commentservice.security.UserPrincipal;
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

    @Override
    public CommentResponse addComment(Long postId, CommentRequest request, UserPrincipal currentUser) {
        if (request.getParentCommentId() != null) {
            Comment parent = findExistingComment(request.getParentCommentId());
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

        return mapToResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getCommentsByPost(Long postId, boolean includePendingForModerator) {
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
    public void likeComment(Long commentId, UserPrincipal currentUser) {
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
        return includePendingForModerator
                ? commentRepository.countByPostId(postId)
                : commentRepository.countByPostIdAndStatus(postId, CommentStatus.APPROVED);
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
}
