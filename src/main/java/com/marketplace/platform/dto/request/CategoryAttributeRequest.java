package com.marketplace.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryAttributeRequest {
    private Long attributeDefinitionId;
    private Boolean isRequired;
    private Integer displayOrder;
    private String defaultValue;
    private ValidationRules validationRules;
}

