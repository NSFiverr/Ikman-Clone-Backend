package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdAttributeResponse {
    private Long attributeDefinitionId;
    private String name;
    private String displayName;
    private String textValue;
    private BigDecimal numericValue;
    private LocalDateTime dateValue;
    private Double latitude;
    private Double longitude;
}
