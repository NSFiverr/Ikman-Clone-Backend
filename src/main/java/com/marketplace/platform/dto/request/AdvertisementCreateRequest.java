package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.advertisement.ItemCondition;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class AdvertisementCreateRequest {
    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Package is required")
    private Long packageId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Location coordinates are required")
    private Double latitude;

    @NotNull(message = "Location coordinates are required")
    private Double longitude;

    @NotBlank(message = "Address is required")
    private String address;

    private Boolean isNegotiable = false;

    @NotNull(message = "Item condition is required")
    private ItemCondition itemCondition;

    private Set<AdAttributeRequest> attributes = new HashSet<>();
    private Set<MediaRequest> mediaItems = new HashSet<>();
}