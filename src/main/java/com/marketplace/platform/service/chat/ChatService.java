package com.marketplace.platform.service.chat;

import com.marketplace.platform.dto.request.ChatMessageRequest;
import com.marketplace.platform.dto.response.ChatMessageResponse;
import com.marketplace.platform.dto.response.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request);
    Page<ChatMessageResponse> getChatMessages(Long chatRoomId, Long requesterId, Pageable pageable);
    List<ChatRoomResponse> getUserChatRooms(Long userId);
    void markMessagesAsRead(Long chatRoomId, Long userId);
    ChatRoomResponse getChatRoomByParticipants(Long participant1Id, Long participant2Id);

    void updateFcmToken(Long userId, String fcmToken);
    String getFcmToken(Long userId);
}