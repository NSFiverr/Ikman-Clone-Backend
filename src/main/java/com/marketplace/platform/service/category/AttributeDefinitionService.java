package com.marketplace.platform.service.category;

import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttributeDefinitionService {
    AttributeDefinitionResponse createAttributeDefinition(AttributeDefinitionCreateRequest request);
     AttributeDefinition getAttributeDefinitionById(Long id);

    AttributeDefinitionResponse getAttributeDefinitionResponseById(Long id);
    Page<AttributeDefinitionResponse> getAllAttributeDefinitions(Pageable pageable);
}
