package com.marketplace.platform.service.chat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.marketplace.platform.domain.interaction.ChatMessage;
import com.marketplace.platform.domain.interaction.ChatRoom;
import com.marketplace.platform.domain.interaction.UserFcmToken;
import com.marketplace.platform.dto.request.ChatMessageRequest;
import com.marketplace.platform.dto.response.ChatMessageResponse;
import com.marketplace.platform.dto.response.ChatRoomResponse;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.exception.AuthenticationException;
import com.marketplace.platform.repository.chat.ChatMessageRepository;
import com.marketplace.platform.repository.chat.ChatRoomRepository;
import com.marketplace.platform.repository.chat.UserFcmTokenRepository;
import com.marketplace.platform.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserFcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        ChatRoom chatRoom = createOrGetChatRoom(senderId, request.getRecipientId(), request.getAdId());

        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoom.getId());
        message.setSenderId(senderId);
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());
        message.setRead(false);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        sendFirebaseNotification(savedMessage, request.getRecipientId());

        return mapToMessageResponse(savedMessage);
    }

    @Override
    public Page<ChatMessageResponse> getChatMessages(Long chatRoomId, Long requesterId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        if (!isParticipant(requesterId, chatRoom)) {
            throw new AuthenticationException("Not authorized to access this chat room");
        }

        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable)
                .map(this::mapToMessageResponse);
    }

    @Override
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        return chatRoomRepository.findByParticipant(userId).stream()
                .map(this::mapToChatRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByChatRoomIdAndReadFalseAndSenderIdNot(chatRoomId, userId);

        unreadMessages.forEach(message -> message.setRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    @Override
    public ChatRoomResponse getChatRoomByParticipants(Long participant1Id, Long participant2Id) {
        return chatRoomRepository.findByParticipants(participant1Id, participant2Id)
                .map(this::mapToChatRoomResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

    private ChatRoom createOrGetChatRoom(Long participant1Id, Long participant2Id, Long adId) {
        return chatRoomRepository.findByParticipants(participant1Id, participant2Id)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setParticipant1Id(participant1Id);
                    newRoom.setParticipant2Id(participant2Id);
                    newRoom.setAdId(adId);
                    newRoom.setCreatedAt(LocalDateTime.now());
                    newRoom.setUpdatedAt(LocalDateTime.now());
                    return chatRoomRepository.save(newRoom);
                });
    }

    @Override
    public void updateFcmToken(Long userId, String fcmToken) {
        UserFcmToken tokenEntity = fcmTokenRepository.findByUserId(userId)
                .orElse(new UserFcmToken());

        tokenEntity.setUserId(userId);
        tokenEntity.setFcmToken(fcmToken);
        tokenEntity.setLastUpdated(LocalDateTime.now());

        fcmTokenRepository.save(tokenEntity);
    }

    @Override
    public String getFcmToken(Long userId) {
        return fcmTokenRepository.findByUserId(userId)
                .map(UserFcmToken::getFcmToken)
                .orElse(null);
    }

    private void sendFirebaseNotification(ChatMessage message, Long recipientId) {
        try {
            String recipientFcmToken = getFcmToken(recipientId);
            if (recipientFcmToken == null) {
                log.warn("No FCM token found for user {}", recipientId);
                return;
            }

            Message fcmMessage = Message.builder()
                    .setToken(recipientFcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("New Message")
                            .setBody(message.getContent())
                            .build())
                    .putData("type", "chat_message")
                    .putData("chatRoomId", message.getChatRoomId().toString())
                    .putData("messageId", message.getId().toString())
                    .build();

            firebaseMessaging.sendAsync(fcmMessage);
        } catch (Exception e) {
            log.error("Failed to send Firebase notification", e);
        }
    }

    private boolean isParticipant(Long userId, ChatRoom chatRoom) {
        return userId.equals(chatRoom.getParticipant1Id()) ||
                userId.equals(chatRoom.getParticipant2Id());
    }

    private ChatMessageResponse mapToMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setChatRoomId(message.getChatRoomId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setRead(message.isRead());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.setId(chatRoom.getId());
        response.setParticipant1Id(chatRoom.getParticipant1Id());
        response.setParticipant2Id(chatRoom.getParticipant2Id());
        response.setAdId(chatRoom.getAdId());
        response.setCreatedAt(chatRoom.getCreatedAt());
        response.setUpdatedAt(chatRoom.getUpdatedAt());

        // Get last message and unread count
        Page<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId(), Pageable.ofSize(1));
        messages.stream().findFirst().ifPresent(
                lastMessage -> response.setLastMessage(mapToMessageResponse(lastMessage))
        );

        return response;
    }
}