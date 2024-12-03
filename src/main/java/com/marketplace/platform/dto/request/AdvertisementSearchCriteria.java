package com.marketplace.platform.dto.request;


import com.marketplace.platform.domain.advertisement.ItemCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementSearchCriteria {
    // Text search
    private String searchTerm;

    // Category filtering
    private Long categoryId;
    private List<Long> categoryIds; // For multiple category search

    // Price range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Location based search
    private Double latitude;
    private Double longitude;
    private Double radius; // in kilometers
    private String location; // text based location (city, area, etc.)

    // Item condition
    private ItemCondition condition;

    // Date filters
    private String postedAfter;
    private String postedBefore;

    // Ad type filters
    private Boolean featured;
    private Boolean topAdsOnly;
    private Boolean negotiable;

    // User related
    private Long sellerId;
    private Boolean verifiedSellersOnly;

    // Sorting
    @Builder.Default
    private String sortBy = "createdAt"; // createdAt, price, viewCount
    @Builder.Default
    private String sortDirection = "DESC"; // ASC, DESC

    // Extra filters that might be useful
    private Integer minViewCount;
    private Boolean hasPhotos;
    private List<String> excludeAdIds; // For excluding already seen ads

    // For dynamic attribute filtering
    private List<AttributeSearchCriteria> attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeSearchCriteria {
        private Long attributeDefinitionId;
        private String textValue;
        private BigDecimal minNumericValue;
        private BigDecimal maxNumericValue;
        private LocalDateTime minDateValue;
        private LocalDateTime maxDateValue;
    }

    // Helper methods for validation and processing
    public boolean hasLocationCriteria() {
        return latitude != null && longitude != null && radius != null;
    }

    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }

    public boolean hasDateRange() {
        return postedAfter != null || postedBefore != null;
    }

    // Method to validate and clean the criteria
    public void validateAndClean() {
        // Ensure radius is positive if location search is used
        if (hasLocationCriteria() && radius <= 0) {
            radius = 10.0; // Default 10km radius
        }

        // Ensure price range is valid
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        // Clean up search term
        if (searchTerm != null) {
            searchTerm = searchTerm.trim();
            if (searchTerm.isEmpty()) {
                searchTerm = null;
            }
        }

        // Validate sort parameters
        if (!isValidSortField(sortBy)) {
            sortBy = "createdAt";
        }
        if (!isValidSortDirection(sortDirection)) {
            sortDirection = "DESC";
        }

        if (postedAfter != null) {
            postedAfter = postedAfter.trim();
            if (postedAfter.isEmpty()) {
                postedAfter = null;
            }
        }
        if (postedBefore != null) {
            postedBefore = postedBefore.trim();
            if (postedBefore.isEmpty()) {
                postedBefore = null;
            }
        }
    }

    private boolean isValidSortField(String field) {
        return field != null && List.of(
                "createdAt", "price", "viewCount", "title"
        ).contains(field.toLowerCase());
    }

    private boolean isValidSortDirection(String direction) {
        return direction != null && List.of(
                "ASC", "DESC"
        ).contains(direction.toUpperCase());
    }
}
