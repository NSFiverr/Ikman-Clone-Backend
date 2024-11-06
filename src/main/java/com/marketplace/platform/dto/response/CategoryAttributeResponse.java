package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.category.FieldType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryAttributeResponse {
    private Long categoryId;
    private Long attrDefId;
    private String attributeName;
    private String attributeDisplayName;
    private FieldType dataType;
    private Boolean isRequired;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
