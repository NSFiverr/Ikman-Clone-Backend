package com.marketplace.platform.repository.chat;

import com.marketplace.platform.domain.interaction.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "(cr.participant1Id = :userId1 AND cr.participant2Id = :userId2) OR " +
            "(cr.participant1Id = :userId2 AND cr.participant2Id = :userId1)")
    Optional<ChatRoom> findByParticipants(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "cr.participant1Id = :userId OR cr.participant2Id = :userId")
    List<ChatRoom> findByParticipant(@Param("userId") Long userId);
}
