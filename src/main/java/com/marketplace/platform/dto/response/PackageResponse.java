package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.advertisement.VisibilityLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PackageResponse {
    private Long packageId;
    private String name;
    private BigDecimal price;
    private Integer durationDays;
    private Integer maxMediaItems;
    private Boolean hasFeaturedListing;
    private Boolean hasTopAd;
    private VisibilityLevel visibilityLevel;
}
