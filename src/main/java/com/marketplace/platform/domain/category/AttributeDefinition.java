package com.marketplace.platform.domain.category;

import com.marketplace.platform.domain.advertisement.AdAttribute;
import com.marketplace.platform.domain.search.SearchIndex;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "attribute_definitions")
@NoArgsConstructor
public class AttributeDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attr_def_id")
    private Long attrDefId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private FieldType dataType;

    @Column(name = "is_searchable")
    private Boolean isSearchable;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @OneToMany(mappedBy = "attributeDefinition")
    private Set<CategoryAttribute> categoryAttributes = new HashSet<>();

    @OneToMany(mappedBy = "attributeDefinition")
    private Set<AdAttribute> adAttributes = new HashSet<>();

    @OneToMany(mappedBy = "attributeDefinition")
    private Set<SearchIndex> searchIndices = new HashSet<>();


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

