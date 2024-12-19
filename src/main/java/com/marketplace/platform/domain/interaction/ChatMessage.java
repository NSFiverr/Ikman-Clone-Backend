package com.marketplace.platform.domain.interaction;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String content;

    private boolean read;
    private LocalDateTime createdAt;

    @Column(name = "firebase_message_id")
    private String firebaseMessageId;
}
