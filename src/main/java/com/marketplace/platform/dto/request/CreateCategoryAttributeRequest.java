package com.marketplace.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCategoryAttributeRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Attribute Definition ID is required")
    private Long attrDefId;

    private Boolean isRequired;
    private Integer displayOrder;
}

