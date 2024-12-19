package com.marketplace.platform.repository.chat;

import com.marketplace.platform.domain.interaction.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m " +
            "JOIN m.conversation c " +
            "JOIN c.participants p " +
            "WHERE p.user.userId = :userId " +
            "AND m.sender.userId != :userId " +
            "AND (p.lastReadAt IS NULL OR m.createdAt > p.lastReadAt)")
    int countAllUnreadMessagesForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.sender.userId != :userId")
    void markMessagesAsRead(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.sender.userId != :userId " +
            "AND m.isRead = false")
    int countUnreadMessages(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId);
}