package com.marketplace.platform.dto.request;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private Long createdById;
}
