package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import org.springframework.stereotype.Component;

@Component
public class AttributeDefinitionMapper {

    public AttributeDefinition toEntity(AttributeDefinitionCreateRequest request) {
        AttributeDefinition attributeDefinition = new AttributeDefinition();
        attributeDefinition.setName(request.getName());
        attributeDefinition.setDisplayName(request.getDisplayName());
        attributeDefinition.setDataType(request.getDataType());
        attributeDefinition.setIsSearchable(request.getIsSearchable());
        attributeDefinition.setIsRequired(request.getIsRequired());
        attributeDefinition.setValidationRules(request.getValidationRules());
        return attributeDefinition;
    }

    public AttributeDefinitionResponse toResponse(AttributeDefinition attributeDefinition) {
        return AttributeDefinitionResponse.builder()
                .attrDefId(attributeDefinition.getAttrDefId())
                .name(attributeDefinition.getName())
                .displayName(attributeDefinition.getDisplayName())
                .dataType(attributeDefinition.getDataType())
                .isSearchable(attributeDefinition.getIsSearchable())
                .isRequired(attributeDefinition.getIsRequired())
                .validationRules(attributeDefinition.getValidationRules())
                .createdAt(attributeDefinition.getCreatedAt())
                .build();
    }
}
