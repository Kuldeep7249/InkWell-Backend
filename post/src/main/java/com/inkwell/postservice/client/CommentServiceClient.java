package com.inkwell.postservice.client;

import com.inkwell.postservice.dto.PostAnalyticsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "comment-service", url = "${services.comment.url:http://localhost:8083}")
public interface CommentServiceClient {

    @GetMapping("/api/comments/analytics")
    List<PostAnalyticsResponse> getPostAnalytics(@RequestParam("postIds") List<Long> postIds);
}
