package com.inkwell.notification.controller;

import com.inkwell.notification.dto.*;
import com.inkwell.notification.security.UserPrincipal;
import com.inkwell.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/notifications", "/notification-api"})
@RequiredArgsConstructor
@Tag(name = "Notification APIs", description = "Manage in-app and email notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @Operation(summary = "Send a single notification", description = "Admin only")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .success(true)
                .message("Notification sent successfully")
                .data(notificationService.send(request))
                .build());
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send notifications to many recipients", description = "Admin only")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .message("Bulk notifications sent successfully")
                .data(notificationService.sendBulk(request))
                .build());
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get notifications by recipient")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByRecipient(
            @PathVariable Long recipientId,
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .message("Notifications fetched successfully")
                .data(notificationService.getByRecipient(recipientId, unreadOnly, principal.getUserId(), isAdmin))
                .build());
    }

    @GetMapping("/me")
    @Operation(summary = "Get notifications for the authenticated user")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMine(
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .message("Notifications fetched successfully")
                .data(notificationService.getByRecipient(principal.getUserId(), unreadOnly, principal.getUserId(), false))
                .build());
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .success(true)
                .message("Notification marked as read")
                .data(notificationService.markAsRead(notificationId, principal.getUserId(), isAdmin))
                .build());
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Mark all recipient notifications as read")
    public ResponseEntity<ApiResponse<Object>> markAllRead(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        notificationService.markAllRead(recipientId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("All notifications marked as read").data(null).build());
    }

    @PutMapping("/me/read-all")
    @Operation(summary = "Mark all authenticated user notifications as read")
    public ResponseEntity<ApiResponse<Object>> markMineRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getUserId(), principal.getUserId(), false);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("All notifications marked as read").data(null).build());
    }

    @DeleteMapping("/recipient/{recipientId}/read")
    @Operation(summary = "Delete all read notifications for a recipient")
    public ResponseEntity<ApiResponse<Object>> deleteRead(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        notificationService.deleteRead(recipientId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Read notifications deleted successfully").data(null).build());
    }

    @DeleteMapping("/me/read")
    @Operation(summary = "Delete all read notifications for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> deleteMineRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.deleteRead(principal.getUserId(), principal.getUserId(), false);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Read notifications deleted successfully").data(null).build());
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.<UnreadCountResponse>builder()
                .success(true)
                .message("Unread count fetched successfully")
                .data(notificationService.getUnreadCount(recipientId, principal.getUserId(), isAdmin))
                .build());
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Get unread notification count for the authenticated user")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getMyUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.<UnreadCountResponse>builder()
                .success(true)
                .message("Unread count fetched successfully")
                .data(notificationService.getUnreadCount(principal.getUserId(), principal.getUserId(), false))
                .build());
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Object>> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        notificationService.deleteNotification(notificationId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Notification deleted successfully").data(null).build());
    }

    @PostMapping("/email")
    @Operation(summary = "Send a plain email notification", description = "Admin only")
    public ResponseEntity<ApiResponse<Object>> sendEmail(@Valid @RequestBody EmailNotificationRequest request) {
        notificationService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Email sent successfully").data(null).build());
    }

    @GetMapping("/all")
    @Operation(summary = "Get all notifications", description = "Admin only")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .message("All notifications fetched successfully")
                .data(notificationService.getAll())
                .build());
    }
}
