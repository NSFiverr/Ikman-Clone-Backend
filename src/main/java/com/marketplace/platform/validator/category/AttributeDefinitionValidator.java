package com.marketplace.platform.validator.category;

import com.marketplace.platform.dto.request.AttributeDefinitionCreateRequest;
import com.marketplace.platform.dto.request.ValidationRules;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.repository.category.AttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@RequiredArgsConstructor
public class AttributeDefinitionValidator {
    private final AttributeDefinitionRepository attributeDefinitionRepository;

    public void validate(AttributeDefinitionCreateRequest request) {
        List<String> errors = new ArrayList<>();

        validateNameUniqueness(request.getName(), errors);
        validateDataTypeRules(request, errors);
        validateCommon(request, errors);

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
            case TEXT -> validateTextType(request, errors);
            case SELECT, MULTI_SELECT -> validateSelectType(request, errors);
            case NUMBER -> validateNumberType(request, errors);
            case DATE -> validateDateType(request, errors);
            // Other cases will be validated by common validation
        }
    }

    private void validateTextType(AttributeDefinitionCreateRequest request, List<String> errors) {
        ValidationRules rules = request.getValidationRules();
        if (rules != null) {
            // Validate min/max length
            if (rules.getMinLength() != null && rules.getMaxLength() != null) {
                if (rules.getMinLength() > rules.getMaxLength()) {
                    errors.add("Minimum length cannot be greater than maximum length");
                }
                if (rules.getMinLength() < 0) {
                    errors.add("Minimum length cannot be negative");
                }
            }

            // Validate pattern if present
            if (rules.getPattern() != null) {
                try {
                    Pattern.compile(rules.getPattern());
                } catch (PatternSyntaxException e) {
                    errors.add("Invalid regular expression pattern");
                }
            }
        }
    }

    private void validateSelectType(AttributeDefinitionCreateRequest request, List<String> errors) {
        ValidationRules rules = request.getValidationRules();
        if (rules == null) {
            errors.add("Validation rules are required for SELECT/MULTI_SELECT types");
        } else if (rules.getOptions() == null || rules.getOptions().isEmpty()) {
            errors.add("Options list cannot be empty for SELECT/MULTI_SELECT types");
        } else {
            // Validate individual options
            Set<String> uniqueOptions = new HashSet<>();
            for (String option : rules.getOptions()) {
                if (option == null || option.trim().isEmpty()) {
                    errors.add("Option values cannot be null or empty");
                } else if (!uniqueOptions.add(option.trim())) {
                    errors.add("Duplicate option found: " + option);
                }
            }
        }
    }

    private void validateNumberType(AttributeDefinitionCreateRequest request, List<String> errors) {
        ValidationRules rules = request.getValidationRules();
        if (rules != null) {
            // Validate min/max values
            if (rules.getMinValue() != null && rules.getMaxValue() != null) {
                if (rules.getMinValue() > rules.getMaxValue()) {
                    errors.add("Minimum value cannot be greater than maximum value");
                }
            }

            // Validate decimals
            if (rules.getDecimals() != null) {
                if (rules.getDecimals() < 0) {
                    errors.add("Decimal places cannot be negative");
                }
            }
        }
    }

    private void validateDateType(AttributeDefinitionCreateRequest request, List<String> errors) {
        ValidationRules rules = request.getValidationRules();
        if (rules != null) {
            if (rules.getMinDate() != null && rules.getMaxDate() != null) {
                try {
                    LocalDate minDate = LocalDate.parse(rules.getMinDate());
                    LocalDate maxDate = LocalDate.parse(rules.getMaxDate());

                    if (minDate.isAfter(maxDate)) {
                        errors.add("Minimum date cannot be after maximum date");
                    }
                } catch (DateTimeParseException e) {
                    errors.add("Invalid date format. Use ISO format (YYYY-MM-DD)");
                }
            }
        }
    }

    private void validateCommon(AttributeDefinitionCreateRequest request, List<String> errors) {
        // Name validation
        if (request.getName() != null && request.getName().length() > 50) {
            errors.add("Name cannot be longer than 50 characters");
        }

        // Display name validation
        if (request.getDisplayName() != null && request.getDisplayName().length() > 100) {
            errors.add("Display name cannot be longer than 100 characters");
        }

        // Error message validation if present
        ValidationRules rules = request.getValidationRules();
        if (rules != null && rules.getErrorMessage() != null && rules.getErrorMessage().length() > 200) {
            errors.add("Error message cannot be longer than 200 characters");
        }
    }
}