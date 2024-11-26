package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.category.FieldType;
import com.marketplace.platform.dto.request.ValidationRules;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AttributeDefinitionResponse {
    private Long attrDefId;
    private String name;
    private String displayName;
    private FieldType dataType;
    private Boolean isSearchable;
    private Boolean isRequired;
    private ValidationRules validationRules;
    private LocalDateTime createdAt;
}
