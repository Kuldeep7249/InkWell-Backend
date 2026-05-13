package com.inkwell.media.client;

import com.inkwell.media.dto.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "post-service", url = "${services.post.url:http://localhost:8082}")
public interface PostServiceClient {

    @GetMapping("/api/posts/public/{postId}")
    PostResponse getPublicPost(@PathVariable Long postId);

    @GetMapping("/api/posts/{postId}")
    PostResponse getAccessiblePost(
            @PathVariable Long postId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    );
}
