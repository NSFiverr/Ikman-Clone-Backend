package com.marketplace.platform.domain.category;

import com.marketplace.platform.domain.advertisement.Advertisement;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "category_versions")
@Data
public class CategoryVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private CategoryStatus status;

    @ElementCollection
    @CollectionTable(
            name = "category_version_attributes",
            joinColumns = @JoinColumn(name = "category_version_id")
    )
    private Set<CategoryVersionAttribute> attributes = new HashSet<>();

    // Track which ads use this version
    @OneToMany(mappedBy = "categoryVersion")
    private Set<Advertisement> advertisements = new HashSet<>();
}

