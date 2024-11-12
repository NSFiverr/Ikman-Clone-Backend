package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryStatistics {
    private Long totalAds;
    private Long totalViews;
    private Long activeAds;
    private Long totalAttributes;
    private LocalDateTime lastAdCreated;
    private LocalDateTime lastModified;
}

