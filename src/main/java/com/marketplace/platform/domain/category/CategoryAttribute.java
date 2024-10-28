package com.marketplace.platform.domain.category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "category_attributes")
@NoArgsConstructor
public class CategoryAttribute {
    @EmbeddedId
    private CategoryAttributeKey id;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attrDefId")
    @JoinColumn(name = "attr_def_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CategoryAttributeKey implements Serializable {
        @Column(name = "category_id")
        private Long categoryId;

        @Column(name = "attr_def_id")
        private Long attrDefId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

