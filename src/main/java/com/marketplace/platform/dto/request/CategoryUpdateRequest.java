package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.category.CategoryStatus;
import lombok.Data;
import java.util.Set;

@Data
public class CategoryUpdateRequest {
    private String name;
    private String description;
    private CategoryStatus status;
    private Set<CategoryAttributeRequest> attributes;
}
