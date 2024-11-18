package com.marketplace.platform.domain.advertisement;

import com.marketplace.platform.domain.category.AttributeDefinition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ad_attributes",
        indexes = {
                @Index(name = "idx_ad_attr_ad", columnList = "ad_id"),
                @Index(name = "idx_ad_attr_def", columnList = "attr_def_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ad_attribute",
                        columnNames = {"ad_id", "attr_def_id"}
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AdAttribute {
    @EmbeddedId
    @Builder.Default
    private AdAttributeKey id = new AdAttributeKey();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("adId")
    @JoinColumn(name = "ad_id", nullable = false)
    private Advertisement advertisement;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attrDefId")
    @JoinColumn(name = "attr_def_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "numeric_value", precision = 19, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "date_value")
    private LocalDateTime dateValue;

    @Column(name = "geo_value")
    private Point locationCoordinates;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = new AdAttributeKey();
        }
        if (this.advertisement != null && this.attributeDefinition != null) {
            this.id.setAdId(this.advertisement.getAdId());
            this.id.setAttrDefId(this.attributeDefinition.getAttrDefId());
        }
    }
}