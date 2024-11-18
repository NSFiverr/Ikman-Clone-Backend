package com.marketplace.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketplace.platform.domain.advertisement.MediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MediaRequest {
    @JsonIgnore
    @NotNull(message = "File is required")
    private MultipartFile file;

    @NotNull(message = "Media type is required")
    private MediaType mediaType;

    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder;
}
