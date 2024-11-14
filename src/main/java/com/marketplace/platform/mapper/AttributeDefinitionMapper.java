package com.marketplace.platform.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.dto.request.ValidationRules;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttributeDefinitionMapper {

    private final ObjectMapper objectMapper;

    public AttributeDefinition toEntity(AttributeDefinitionCreateRequest request) {
        try {
            AttributeDefinition attributeDefinition = new AttributeDefinition();
            attributeDefinition.setName(request.getName());
            attributeDefinition.setDisplayName(request.getDisplayName());
            attributeDefinition.setDataType(request.getDataType());
            attributeDefinition.setIsSearchable(request.getIsSearchable());
            attributeDefinition.setIsRequired(request.getIsRequired());

            // Convert ValidationRules object to JSON string
            String validationRulesJson = null;
            if (request.getValidationRules() != null) {
                validationRulesJson = objectMapper.writeValueAsString(request.getValidationRules());
            }
            attributeDefinition.setValidationRules(validationRulesJson);

            return attributeDefinition;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid validation rules format");
        }
    }

    public AttributeDefinitionResponse toResponse(AttributeDefinition attributeDefinition) {
        try {
            // Convert JSON string back to ValidationRules object
            ValidationRules validationRules = null;
            if (attributeDefinition.getValidationRules() != null) {
                validationRules = objectMapper.readValue(
                        attributeDefinition.getValidationRules(),
                        ValidationRules.class
                );
            }

            return AttributeDefinitionResponse.builder()
                    .attrDefId(attributeDefinition.getAttrDefId())
                    .name(attributeDefinition.getName())
                    .displayName(attributeDefinition.getDisplayName())
                    .dataType(attributeDefinition.getDataType())
                    .isSearchable(attributeDefinition.getIsSearchable())
                    .isRequired(attributeDefinition.getIsRequired())
                    .validationRules(validationRules)
                    .createdAt(attributeDefinition.getCreatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Error parsing validation rules");
        }
    }
}