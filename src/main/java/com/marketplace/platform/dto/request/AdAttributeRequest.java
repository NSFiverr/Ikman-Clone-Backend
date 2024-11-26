package com.marketplace.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdAttributeRequest {
    @NotNull(message = "Attribute definition is required")
    private Long attributeDefinitionId;

    private String textValue;
    private BigDecimal numericValue;
    private LocalDateTime dateValue;
    private Double latitude;
    private Double longitude;
}
