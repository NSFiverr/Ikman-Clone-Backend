package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.advertisement.VisibilityLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdPackageResponse {
    private Long packageId;
    private String name;
    private BigDecimal price;
    private Integer durationDays;
    private Integer maxMediaItems;
    private Integer maxDocuments;
    private Boolean hasRenewalOption;
    private Boolean hasFeaturedListing;
    private Boolean hasTopAd;
    private Boolean hasPrioritySupport;
    private VisibilityLevel visibilityLevel;
    private Integer featuredDurationDays;
    private LocalDateTime createdAt;
}
