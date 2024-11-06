package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.category.FieldType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttributeDefinitionResponse {
    private Long attrDefId;
    private String name;
    private String displayName;
    private FieldType dataType;
    private Boolean isSearchable;
    private Boolean isRequired;
    private String validationRules;
    private LocalDateTime createdAt;

}
