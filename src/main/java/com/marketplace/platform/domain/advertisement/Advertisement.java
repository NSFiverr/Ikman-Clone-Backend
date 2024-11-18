package com.marketplace.platform.domain.advertisement;

import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.category.CategoryVersion;
import com.marketplace.platform.domain.interaction.AdView;
import com.marketplace.platform.domain.interaction.Conversation;
import com.marketplace.platform.domain.interaction.Message;
import com.marketplace.platform.domain.interaction.UserFavorite;
import com.marketplace.platform.domain.search.SearchIndex;
import com.marketplace.platform.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "advertisements", indexes = {
        @Index(name = "idx_ads_status_category", columnList = "ad_status,category_version_id"),
        @Index(name = "idx_ads_price", columnList = "price"),
//       Todo :  @Index(name = "idx_ads_location", columnList = "location_coordinates")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"mediaItems", "attributes", "favorites", "conversations", "views", "searchIndices"})
public class Advertisement {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long adId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private AdPackage adPackage;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "location_coordinates", columnDefinition = "GEOMETRY NOT NULL")
    @Builder.Default
    private Point locationCoordinates = geometryFactory.createPoint(new Coordinate(0, 0));

    private String address;

    @Column(name = "is_negotiable")
    @Builder.Default
    private Boolean isNegotiable = false;

    @Column(name = "item_condition")
    @Enumerated(EnumType.STRING)
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_status")
    @Builder.Default
    private AdStatus status = AdStatus.DRAFT;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_top_ad")
    @Builder.Default
    private Boolean isTopAd = false;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "featured_until")
    private LocalDateTime featuredUntil;

    @Column(name = "top_ad_until")
    private LocalDateTime topAdUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL)
    private Set<AdMedia> mediaItems = new HashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL)
    private Set<AdAttribute> attributes = new HashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement")
    private Set<UserFavorite> favorites = new HashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement")
    private Set<Conversation> conversations = new HashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement")
    private Set<AdView> views = new HashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "advertisement")
    private Set<SearchIndex> searchIndices = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_version_id", nullable = false)
    private CategoryVersion categoryVersion;

    @PrePersist
    protected void onCreate() {
        if (categoryVersion == null) {
            throw new IllegalStateException("Advertisement must have a category version");
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (locationCoordinates == null) {
            locationCoordinates = geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Category getCategory() {
        return categoryVersion != null ? categoryVersion.getCategory() : null;
    }
}