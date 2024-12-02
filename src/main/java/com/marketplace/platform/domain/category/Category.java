package com.marketplace.platform.domain.category;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "categories")
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    // Basic metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Admin createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    // Path materialization for efficient tree traversal
    @Column(name = "tree_path", length = 1000)
    private String treePath;

    @Column(name = "depth")
    private Integer depth = 0;

    // Hierarchical structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Category parent;

    @OneToMany(mappedBy = "parent")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Category> children = new HashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @OrderBy("versionNumber DESC")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CategoryVersion> versions = new HashSet<>();


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public CategoryVersion getCurrentVersion() {
        return versions.stream()
                .filter(v -> v.getValidTo() == null)
                .findFirst()
                .orElse(null);
    }

    // Convenience method to get name from current version
    @Transient
    public String getName() {
        CategoryVersion currentVersion = getCurrentVersion();
        return currentVersion != null ? currentVersion.getName() : null;
    }

    // Convenience method to get description from current version
    @Transient
    public String getDescription() {
        CategoryVersion currentVersion = getCurrentVersion();
        return currentVersion != null ? currentVersion.getDescription() : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = CategoryStatus.ACTIVE;

        // Initialize tree path if parent exists
        updateTreePath();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update tree path if parent has changed
        updateTreePath();
    }

    private void updateTreePath() {
        if (parent != null) {
            this.treePath = parent.getTreePath() == null ?
                    parent.getCategoryId().toString() :
                    parent.getTreePath() + "." + parent.getCategoryId();
            this.depth = parent.getDepth() + 1;
        } else {
            this.treePath = "";
            this.depth = 0;
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

    public void addVersion(CategoryVersion version) {
        versions.add(version);
        version.setCategory(this);
    }
}