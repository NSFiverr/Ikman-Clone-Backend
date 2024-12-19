package com.marketplace.platform.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}