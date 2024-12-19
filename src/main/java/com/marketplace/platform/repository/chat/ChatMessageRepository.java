package com.marketplace.platform.repository.chat;

import com.marketplace.platform.domain.interaction.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);
    List<ChatMessage> findByChatRoomIdAndReadFalseAndSenderIdNot(Long chatRoomId, Long userId);
}
