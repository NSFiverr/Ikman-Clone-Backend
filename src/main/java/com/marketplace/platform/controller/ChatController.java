package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.ChatMessageRequest;
import com.marketplace.platform.dto.request.FcmTokenRequest;
import com.marketplace.platform.dto.response.ChatMessageResponse;
import com.marketplace.platform.dto.response.ChatRoomResponse;
import com.marketplace.platform.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(userId, request));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getChatMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getChatMessages(roomId, userId, pageable));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getUserChatRooms(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getUserChatRooms(userId));
    }

    @PatchMapping("/rooms/{roomId}/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        chatService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms/find")
    public ResponseEntity<ChatRoomResponse> findChatRoom(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long otherUserId) {
        return ResponseEntity.ok(
                chatService.getChatRoomByParticipants(userId, otherUserId)
        );
    }

    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        chatService.updateFcmToken(userId, request.getToken());
        return ResponseEntity.ok().build();
    }
}