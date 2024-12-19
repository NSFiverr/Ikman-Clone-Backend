package com.marketplace.platform.domain.interaction;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long participant1Id;

    @Column(nullable = false)
    private Long participant2Id;

    @Column(nullable = true)  // Optional, if you want to link chats to ads
    private Long adId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


