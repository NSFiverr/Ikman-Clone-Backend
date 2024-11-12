package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.category.CategoryStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CategorySearchCriteria {
    private String name;
    private Long parentId;
    private CategoryStatus status;
    private Integer depth;
    private Boolean includeDeleted = false;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minAttributeCount;
    private Set<Long> attributeDefinitionIds;
    private Boolean hasActiveAds;
}
