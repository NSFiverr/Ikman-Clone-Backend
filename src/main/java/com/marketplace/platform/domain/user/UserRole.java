package com.marketplace.platform.domain.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "user_roles",
        indexes = {
                @Index(name = "idx_user_role_user", columnList = "user_id"),
                @Index(name = "idx_user_role_name", columnList = "role_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_role",
                        columnNames = {"user_id", "role_name"}
                )
        }
)
@NoArgsConstructor
@EqualsAndHashCode(of = {"user", "roleName"})
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleName roleName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
