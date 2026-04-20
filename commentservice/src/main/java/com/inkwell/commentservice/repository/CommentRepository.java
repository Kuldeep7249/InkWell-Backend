package com.inkwell.commentservice.repository;

import com.inkwell.commentservice.entity.Comment;
import com.inkwell.commentservice.entity.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    List<Comment> findByPostIdAndStatus(Long postId, CommentStatus status);

    List<Comment> findByAuthorId(Long authorId);

    Optional<Comment> findByCommentId(Long commentId);

    List<Comment> findByParentCommentId(Long parentCommentId);

    List<Comment> findByParentCommentIdAndStatus(Long parentCommentId, CommentStatus status);

    List<Comment> findByPostIdAndParentCommentIdIsNull(Long postId);

    List<Comment> findByPostIdAndParentCommentIdIsNullAndStatus(Long postId, CommentStatus status);

    long countByPostId(Long postId);

    long countByPostIdAndStatus(Long postId, CommentStatus status);

    List<Comment> findByStatus(CommentStatus status);

    void deleteByCommentId(Long commentId);
}
