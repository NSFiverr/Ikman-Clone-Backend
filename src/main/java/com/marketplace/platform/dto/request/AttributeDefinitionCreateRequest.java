package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.category.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttributeDefinitionCreateRequest {

    @NotBlank(message = "Attribute name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Data type is required")
    private FieldType dataType;

    private Boolean isSearchable = false;
    private Boolean isRequired = false;
    private String validationRules;
}



