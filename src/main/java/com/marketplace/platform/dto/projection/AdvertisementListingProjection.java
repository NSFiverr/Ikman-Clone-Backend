package com.marketplace.platform.dto.projection;

import com.marketplace.platform.domain.advertisement.ItemCondition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementListingProjection {
    Long getAdId();
    String getTitle();
    BigDecimal getPrice();
    String getAddress();
    ItemCondition getItemCondition();
    Boolean getIsNegotiable();
    Boolean getIsFeatured();
    Boolean getIsTopAd();
    Integer getViewCount();
    LocalDateTime getCreatedAt();
    LocalDateTime getExpiresAt();
    String getMediaItems();

    // Category info
    Long getCategoryVersionCategoryId();
    String getCategoryVersionName();
    Integer getCategoryVersionNumber();

    // User info
    Long getUserUserId();
    String getUserFirstName();
    String getUserLastName();
    Boolean getUserIsEmailVerified();
}