package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.category.FieldType;
import com.marketplace.platform.dto.request.ValidationRules;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryAttributeResponse {
    private Long attributeDefinitionId;
    private String attributeName;
    private String displayName;
    private Boolean isRequired;
    private Integer displayOrder;
    private String defaultValue;
    private ValidationRules validationRules;
}