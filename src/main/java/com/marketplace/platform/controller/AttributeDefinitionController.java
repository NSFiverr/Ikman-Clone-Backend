package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.CreateAttributeDefinitionRequest;
import com.marketplace.platform.dto.request.UpdateAttributeDefinitionRequest;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.service.category.AttributeDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeDefinitionController {
    private final AttributeDefinitionService attributeDefinitionService;

    @GetMapping
    public ResponseEntity<List<AttributeDefinitionResponse>> getAllAttributeDefinitions() {
        return ResponseEntity.ok(attributeDefinitionService.getAllAttributeDefinitions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeDefinitionResponse> getAttributeDefinitionById(@PathVariable Long id) {
        return ResponseEntity.ok(attributeDefinitionService.getAttributeDefinitionById(id));
    }

    @PostMapping
    public ResponseEntity<AttributeDefinitionResponse> createAttributeDefinition(
            @Valid @RequestBody CreateAttributeDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeDefinitionService.createAttributeDefinition(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeDefinitionResponse> updateAttributeDefinition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttributeDefinitionRequest request) {
        return ResponseEntity.ok(attributeDefinitionService.updateAttributeDefinition(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttributeDefinition(@PathVariable Long id) {
        attributeDefinitionService.deleteAttributeDefinition(id);
        return ResponseEntity.noContent().build();
    }
}