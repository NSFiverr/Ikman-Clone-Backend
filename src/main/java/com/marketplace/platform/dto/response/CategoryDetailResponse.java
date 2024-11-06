package com.marketplace.platform.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDetailResponse extends CategoryResponse {
    private List<CategoryAttributeResponse> attributes;
}
