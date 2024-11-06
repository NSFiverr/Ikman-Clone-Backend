package com.marketplace.platform.dto.request;

import lombok.Data;

@Data
public class UpdateAttributeDefinitionRequest {
    private String displayName;
    private Boolean isSearchable;
    private Boolean isRequired;
    private String validationRules;
}
