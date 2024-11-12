package com.marketplace.platform.service.category;

import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.mapper.AttributeDefinitionMapper;
import com.marketplace.platform.repository.category.AttributeDefinitionRepository;
import com.marketplace.platform.validator.category.AttributeDefinitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeDefinitionServiceImpl implements AttributeDefinitionService {
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeDefinitionMapper attributeDefinitionMapper;
    private final AttributeDefinitionValidator validator;

    @Override
    @Transactional
    public AttributeDefinitionResponse createAttributeDefinition(AttributeDefinitionCreateRequest request) {
        validator.validate(request);

        AttributeDefinition attributeDefinition = attributeDefinitionMapper.toEntity(request);
        attributeDefinition = attributeDefinitionRepository.save(attributeDefinition);

        return attributeDefinitionMapper.toResponse(attributeDefinition);
    }

    @Override
    public AttributeDefinition getAttributeDefinitionById(Long id) {
        return attributeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found: " + id));
    }
}