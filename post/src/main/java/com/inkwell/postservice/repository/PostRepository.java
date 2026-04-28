package com.inkwell.postservice.repository;

import com.inkwell.postservice.entity.Post;
import com.inkwell.postservice.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId);
    List<Post> findByStatus(PostStatus status);
}
