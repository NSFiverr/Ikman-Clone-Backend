package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.category.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAttributeDefinitionRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String displayName;

    @NotNull(message = "Data type is required")
    private FieldType dataType;

    private Boolean isSearchable;
    private Boolean isRequired;
    private String validationRules;
}
