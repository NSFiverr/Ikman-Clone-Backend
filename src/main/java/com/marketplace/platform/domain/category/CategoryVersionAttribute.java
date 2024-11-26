package com.marketplace.platform.domain.category;

import com.marketplace.platform.dto.request.ValidationRules;
import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class CategoryVersionAttribute {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_definition_id")
    private AttributeDefinition attributeDefinition;

    @Column(nullable = false)
    private Boolean isRequired;

    @Column(nullable = false)
    private Integer displayOrder;

    // Additional useful fields that might be needed
    private String defaultValue;

    @Column(columnDefinition = "TEXT")
    private String validationRules;
}