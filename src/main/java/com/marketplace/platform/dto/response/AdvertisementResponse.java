package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.ItemCondition;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class AdvertisementResponse {
    private Long adId;
    private String title;
    private String description;
    private BigDecimal price;
    private Boolean isNegotiable;
    private ItemCondition itemCondition;
    private AdStatus status;
    private Boolean isFeatured;
    private Boolean isTopAd;
    private Integer viewCount;

    // Location
    private Double latitude;
    private Double longitude;
    private String address;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime featuredUntil;
    private LocalDateTime topAdUntil;

    // Related entities
    private UserResponse user;
    private CategoryResponse category;
    private PackageResponse adPackage;
    private Set<AdAttributeResponse> attributes;
    private Set<MediaResponse> mediaItems;

    // Stats
    private Long favoriteCount;
    private Boolean isFavorited;
    private Boolean isOwner;
}