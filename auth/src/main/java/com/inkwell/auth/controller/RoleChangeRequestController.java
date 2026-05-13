package com.inkwell.auth.controller;

import com.inkwell.auth.dto.CreateRoleChangeRequest;
import com.inkwell.auth.dto.NotificationType;
import com.inkwell.auth.dto.RelatedType;
import com.inkwell.auth.dto.RoleChangeRequestDto;
import com.inkwell.auth.dto.SendNotificationRequest;
import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.RoleChangeRequest;
import com.inkwell.auth.entity.RoleRequestStatus;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.messaging.NotificationEventPublisher;
import com.inkwell.auth.repository.RoleChangeRequestRepository;
import com.inkwell.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth/role-requests")
@RequiredArgsConstructor
public class RoleChangeRequestController {

    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @PostMapping
    public RoleChangeRequestDto createRequest(
            Authentication authentication,
            @Valid @RequestBody CreateRoleChangeRequest request
    ) {
        User user = getAuthenticatedUser(authentication);

        if (request.getRequestedRole() == Role.READER) {
            throw new IllegalArgumentException("You can request AUTHOR or ADMIN role only");
        }

        if (user.getRole() == request.getRequestedRole()) {
            throw new IllegalArgumentException("You already have this role");
        }

        boolean hasPendingRequest = roleChangeRequestRepository.existsByUserAndRequestedRoleAndStatus(
                user,
                request.getRequestedRole(),
                RoleRequestStatus.PENDING
        );

        if (hasPendingRequest) {
            throw new IllegalArgumentException("You already have a pending request for this role");
        }

        RoleChangeRequest roleChangeRequest = RoleChangeRequest.builder()
                .user(user)
                .requestedRole(request.getRequestedRole())
                .reason(request.getReason() == null ? null : request.getReason().trim())
                .status(RoleRequestStatus.PENDING)
                .build();

        return mapToDto(roleChangeRequestRepository.save(roleChangeRequest));
    }

    @GetMapping("/me")
    public List<RoleChangeRequestDto> getMyRequests(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return roleChangeRequestRepository.findByUserOrderByRequestedAtDesc(user)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @GetMapping({"/admin", "/admin/"})
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleChangeRequestDto> getAllRequests(
            @RequestParam(required = false) RoleRequestStatus status
    ) {
        List<RoleChangeRequest> requests = status == null
                ? roleChangeRequestRepository.findAllByOrderByRequestedAtDesc()
                : roleChangeRequestRepository.findByStatusOrderByRequestedAtDesc(status);

        return requests.stream()
                .map(this::mapToDto)
                .toList();
    }

    @PutMapping("/admin/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleChangeRequestDto approveRequest(
            @PathVariable Long requestId,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        User admin = getAuthenticatedUser(authentication);
        RoleChangeRequest request = getPendingRequest(requestId);
        User user = request.getUser();

        user.setRole(request.getRequestedRole());
        userRepository.save(user);

        request.setStatus(RoleRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin.getUserId());
        RoleChangeRequest savedRequest = roleChangeRequestRepository.save(request);
        sendRoleRequestDecisionNotification(savedRequest, admin.getUserId());
        return mapToDto(savedRequest);
    }

    @PutMapping("/admin/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleChangeRequestDto rejectRequest(
            @PathVariable Long requestId,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        User admin = getAuthenticatedUser(authentication);
        RoleChangeRequest request = getPendingRequest(requestId);

        request.setStatus(RoleRequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin.getUserId());
        RoleChangeRequest savedRequest = roleChangeRequestRepository.save(request);
        sendRoleRequestDecisionNotification(savedRequest, admin.getUserId());
        return mapToDto(savedRequest);
    }

    private void sendRoleRequestDecisionNotification(RoleChangeRequest request, Long actorId) {
        String title = request.getStatus() == RoleRequestStatus.APPROVED ? "Role request approved" : "Role request rejected";
        String message = request.getStatus() == RoleRequestStatus.APPROVED
                ? "Your request for " + request.getRequestedRole().name() + " role has been approved."
                : "Your request for " + request.getRequestedRole().name() + " role has been rejected.";

        notificationEventPublisher.publish(SendNotificationRequest.builder()
                .recipientId(request.getUser().getUserId())
                .actorId(actorId)
                .type(NotificationType.SYSTEM_ALERT)
                .title(title)
                .message(message)
                .relatedId(request.getRequestId())
                .relatedType(RelatedType.USER)
                .sendEmail(false)
                .build());
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private RoleChangeRequest getPendingRequest(Long requestId) {
        RoleChangeRequest request = roleChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Role request not found"));

        if (request.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalArgumentException("This request has already been reviewed");
        }

        return request;
    }

    private RoleChangeRequestDto mapToDto(RoleChangeRequest request) {
        User user = request.getUser();

        return RoleChangeRequestDto.builder()
                .requestId(request.getRequestId())
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .currentRole(user.getRole())
                .requestedRole(request.getRequestedRole())
                .status(request.getStatus())
                .reason(request.getReason())
                .requestedAt(request.getRequestedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy())
                .build();
    }
}
