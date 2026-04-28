package com.inkwell.postservice.client;

import com.inkwell.postservice.dto.AuthProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${services.auth.url:http://localhost:8080}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/profile")
    AuthProfileResponse getProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader);
}
