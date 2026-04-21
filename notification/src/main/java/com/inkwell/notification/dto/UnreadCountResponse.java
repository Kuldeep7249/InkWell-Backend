package com.inkwell.notification.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {
    private Long recipientId;
    private long unreadCount;
}
