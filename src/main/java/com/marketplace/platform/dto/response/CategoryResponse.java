package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.category.CategoryStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class CategoryResponse {
    private Long categoryId;
    private String name;
    private String description;
    private String treePath;
    private Integer depth;
    private Long parentId;
    private String parentName;
    private Integer versionNumber;
    private CategoryStatus status;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Set<CategoryAttributeResponse> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}