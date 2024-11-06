package com.marketplace.platform.service.category;

import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.dto.request.CreateAttributeDefinitionRequest;
import com.marketplace.platform.dto.request.UpdateAttributeDefinitionRequest;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.category.AttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributeDefinitionService {
    private final AttributeDefinitionRepository attributeDefinitionRepository;

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> getAllAttributeDefinitions() {
        return attributeDefinitionRepository.findAll().stream()
                .map(this::convertToAttributeDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttributeDefinitionResponse getAttributeDefinitionById(Long id) {
        AttributeDefinition definition = attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute Definition not found with id: " + id));
        return convertToAttributeDefinitionResponse(definition);
    }

    @Transactional
    public AttributeDefinitionResponse createAttributeDefinition(CreateAttributeDefinitionRequest request) {
        AttributeDefinition definition = new AttributeDefinition();
        definition.setName(request.getName());
        definition.setDisplayName(request.getDisplayName());
        definition.setDataType(request.getDataType());
        definition.setIsSearchable(request.getIsSearchable());
        definition.setIsRequired(request.getIsRequired());
        definition.setValidationRules(request.getValidationRules());
        return convertToAttributeDefinitionResponse(attributeDefinitionRepository.save(definition));
    }

    @Transactional
    public AttributeDefinitionResponse updateAttributeDefinition(Long id, UpdateAttributeDefinitionRequest request) {
        AttributeDefinition definition = attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute Definition not found with id: " + id));

        if (request.getDisplayName() != null) {
            definition.setDisplayName(request.getDisplayName());
        }
        if (request.getIsSearchable() != null) {
            definition.setIsSearchable(request.getIsSearchable());
        }
        if (request.getIsRequired() != null) {
            definition.setIsRequired(request.getIsRequired());
        }
        if (request.getValidationRules() != null) {
            definition.setValidationRules(request.getValidationRules());
        }

        return convertToAttributeDefinitionResponse(attributeDefinitionRepository.save(definition));
    }

    @Transactional
    public void deleteAttributeDefinition(Long id) {
        AttributeDefinition definition = attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute Definition not found with id: " + id));
        attributeDefinitionRepository.delete(definition);
    }

    private AttributeDefinitionResponse convertToAttributeDefinitionResponse(AttributeDefinition definition) {
        AttributeDefinitionResponse response = new AttributeDefinitionResponse();
        response.setAttrDefId(definition.getAttrDefId());
        response.setName(definition.getName());
        response.setDisplayName(definition.getDisplayName());
        response.setDataType(definition.getDataType());
        response.setIsSearchable(definition.getIsSearchable());
        response.setIsRequired(definition.getIsRequired());
        response.setValidationRules(definition.getValidationRules());
        response.setCreatedAt(definition.getCreatedAt());
        return response;
    }
}

