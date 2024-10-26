package com.marketplace.platform.repository.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("""
        SELECT u FROM User u 
        WHERE (:term IS NULL OR 
              LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))
        AND (:status IS NULL OR u.status = :status)
        AND (CAST(:startDate AS timestamp) IS NULL OR u.createdAt >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR u.createdAt <= :endDate)
        """)
    Page<User> searchUsers(
            @Param("term") String term,
            @Param("status") UserStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
