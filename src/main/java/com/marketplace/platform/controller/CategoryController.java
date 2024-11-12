package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.service.category.AttributeDefinitionService;
import com.marketplace.platform.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @ModelAttribute CategorySearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategories(criteria, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<CategoryResponse> restoreCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.restoreCategory(id));
    }

    // Attribute Definition endpoints
    @PostMapping("/attributes/definitions")
    public ResponseEntity<AttributeDefinitionResponse> createAttributeDefinition(
            @Valid @RequestBody AttributeDefinitionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeDefinitionService.createAttributeDefinition(request));
    }
}