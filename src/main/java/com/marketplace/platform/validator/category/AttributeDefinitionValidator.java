package com.marketplace.platform.validator.category;

import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.repository.category.AttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttributeDefinitionValidator {
    private final AttributeDefinitionRepository attributeDefinitionRepository;

    public void validate(AttributeDefinitionCreateRequest request) {
        List<String> errors = new ArrayList<>();

        validateNameUniqueness(request.getName(), errors);
        validateDataTypeRules(request, errors);

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join(", ", errors));
        }
    }

    private void validateNameUniqueness(String name, List<String> errors) {
        if (attributeDefinitionRepository.existsByNameIgnoreCase(name)) {
            errors.add("Attribute definition with this name already exists");
        }
    }

    private void validateDataTypeRules(AttributeDefinitionCreateRequest request, List<String> errors) {
        switch (request.getDataType()) {
            case SELECT, MULTI_SELECT -> validateSelectType(request, errors);
            case NUMBER -> validateNumberType(request, errors);
            case DATE -> validateDateType(request, errors);
            // Add more cases as needed
        }
    }

    private void validateSelectType(AttributeDefinitionCreateRequest request, List<String> errors) {
        if (request.getValidationRules() == null || request.getValidationRules().isEmpty()) {
            errors.add("Validation rules (options) are required for SELECT/MULTI_SELECT types");
        } else {
            // TODO: Validate JSON format and options structure
            // Example: Check if the JSON is valid and contains an "options" array
        }
    }

    private void validateNumberType(AttributeDefinitionCreateRequest request, List<String> errors) {
        if (request.getValidationRules() != null) {
            // TODO: Validate min/max values
            // Example: Check if min is less than max, values are numeric, etc.
        }
    }

    private void validateDateType(AttributeDefinitionCreateRequest request, List<String> errors) {
        if (request.getValidationRules() != null) {
            // TODO: Validate date range rules
            // Example: Check if min date is before max date, format is valid, etc.
        }
    }
}
