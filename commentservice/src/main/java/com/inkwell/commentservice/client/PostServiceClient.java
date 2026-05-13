package com.inkwell.commentservice.client;

import com.inkwell.commentservice.dto.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", url = "${services.post.url:http://localhost:8082}")
public interface PostServiceClient {

    @GetMapping("/api/posts/public/{postId}")
    PostResponse getPublicPost(@PathVariable Long postId);
}
