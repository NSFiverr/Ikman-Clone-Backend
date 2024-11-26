package com.marketplace.platform.domain.advertisement;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "ad_packages")
@NoArgsConstructor
public class AdPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "max_media_items")
    private Integer maxMediaItems;

    @Column(name = "max_documents")
    private Integer maxDocuments;

    @Column(name = "has_renewal_option")
    private Boolean hasRenewalOption;

    @Column(name = "has_featured_listing")
    private Boolean hasFeaturedListing;

    @Column(name = "has_top_ad")
    private Boolean hasTopAd;

    @Column(name = "has_priority_support")
    private Boolean hasPrioritySupport;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_level", nullable = false)
    private VisibilityLevel visibilityLevel = VisibilityLevel.PUBLIC;

    @Column(name = "featured_duration_days")
    private Integer featuredDurationDays;

    @OneToMany(mappedBy = "adPackage")
    private Set<Advertisement> advertisements = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
