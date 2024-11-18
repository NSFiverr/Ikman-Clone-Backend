package com.marketplace.platform.dto.request;


import com.marketplace.platform.domain.advertisement.VisibilityLevel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdPackageCreateRequest {
    @NotBlank(message = "Package name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    @NotNull(message = "Max media items is required")
    @Min(value = 1, message = "Must allow at least 1 media item")
    private Integer maxMediaItems;

    @Min(value = 0, message = "Max documents cannot be negative")
    private Integer maxDocuments;

    private Boolean hasRenewalOption = false;
    private Boolean hasFeaturedListing = false;
    private Boolean hasTopAd = false;
    private Boolean hasPrioritySupport = false;

    @NotNull(message = "Visibility level is required")
    private VisibilityLevel visibilityLevel;

    private Integer featuredDurationDays;
}
