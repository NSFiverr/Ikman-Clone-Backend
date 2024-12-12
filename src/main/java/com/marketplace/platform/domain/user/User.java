package com.marketplace.platform.domain.user;

import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.interaction.*;
import com.marketplace.platform.domain.token.PasswordResetToken;
import com.marketplace.platform.domain.token.VerificationToken;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "user_status"),
        @Index(name = "idx_user_created_at", columnList = "created_at"),
        @Index(name = "idx_user_last_login", columnList = "last_login_at")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @ToString.Exclude
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "display_phone", nullable = false)
    @Builder.Default
    private Boolean displayPhone = true;

    @Column(name = "display_email", nullable = false)
    @Builder.Default
    private Boolean displayEmail = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Relationships from User
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Advertisement> advertisements = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<UserFavorite> favorites = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<AdView> adViews = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<ConversationParticipant> conversations = new HashSet<>();


    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @Builder.Default
    private Set<VerificationToken> verificationTokens = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    @Builder.Default
    private Set<PasswordResetToken> passwordResetTokens = new HashSet<>();
}