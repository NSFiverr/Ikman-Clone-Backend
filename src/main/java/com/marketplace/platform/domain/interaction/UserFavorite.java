package com.marketplace.platform.domain.interaction;

import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_favorite_ad",
                        columnNames = {"user_id", "ad_id"}
                )
        },
        indexes = {
                @Index(name = "idx_favorites_user_id", columnList = "user_id"),
                @Index(name = "idx_favorites_ad_id", columnList = "ad_id"),
                @Index(name = "idx_favorites_created_at", columnList = "created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Advertisement advertisement;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

