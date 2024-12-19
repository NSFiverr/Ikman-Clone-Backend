package com.marketplace.platform.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRoomResponse {
    private Long id;
    private Long participant1Id;
    private Long participant2Id;
    private Long adId;
    private ChatMessageResponse lastMessage;
    private int unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}