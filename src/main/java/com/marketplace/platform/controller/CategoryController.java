package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.service.category.AttributeDefinitionService;
import com.marketplace.platform.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final AttributeDefinitionService attributeDefinitionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @ModelAttribute CategorySearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategories(criteria, pageable));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<CategoryResponse>> getCategoryVersionHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "versionNumber", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(categoryService.getCategoryVersionHistory(id, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> restoreCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.restoreCategory(id));
    }

    // Attribute Definition endpoints
    @PostMapping("/attributes/definitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AttributeDefinitionResponse> createAttributeDefinition(
            @Valid @RequestBody AttributeDefinitionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeDefinitionService.createAttributeDefinition(request));
    }

    @GetMapping("/attributes/definitions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttributeDefinitionResponse> getAttributeDefinition(@PathVariable Long id) {
        return ResponseEntity.ok(attributeDefinitionService.getAttributeDefinitionResponseById(id));
    }

    @GetMapping("/attributes/definitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AttributeDefinitionResponse>> getAllAttributeDefinitions(Pageable pageable) {
        return ResponseEntity.ok(attributeDefinitionService.getAllAttributeDefinitions(pageable));
    }
}