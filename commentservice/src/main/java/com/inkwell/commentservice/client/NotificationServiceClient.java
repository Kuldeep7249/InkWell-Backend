package com.inkwell.commentservice.client;

import com.inkwell.commentservice.dto.NotificationResponse;
import com.inkwell.commentservice.dto.SendNotificationRequest;
import com.inkwell.commentservice.dto.ServiceApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", url = "${services.notification.url:http://localhost:8087}")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/send")
    ServiceApiResponse<NotificationResponse> send(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                  @RequestBody SendNotificationRequest request);
}
