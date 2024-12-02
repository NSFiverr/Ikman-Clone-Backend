package com.marketplace.platform.domain.search;

import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.category.AttributeDefinition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "search_index",
        indexes = {
                @Index(name = "idx_search_numeric", columnList = "attr_def_id,numeric_value"),
                @Index(name = "idx_search_date", columnList = "attr_def_id,date_value"),
                @Index(name = "idx_search_text", columnList = "search_vector")

                //  @Index(name = "idx_search_geo", columnList = "geo_value", columnDefinition = "SPATIAL"),
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIndex {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @EmbeddedId
    private SearchIndexKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("adId")
    @JoinColumn(name = "ad_id")
    private Advertisement advertisement;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attrDefId")
    @JoinColumn(name = "attr_def_id")
    private AttributeDefinition attributeDefinition;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "numeric_value", precision = 38, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "date_value")
    private LocalDateTime dateValue;

    @Column(name = "geo_value", columnDefinition = "GEOMETRY NOT NULL")
    @Builder.Default
    private Point geoValue = geometryFactory.createPoint(new Coordinate(0, 0));

    @Column(name = "search_vector", columnDefinition = "VARCHAR(255)")
    private String searchVector;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (geoValue == null) {
            geoValue = geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }
}