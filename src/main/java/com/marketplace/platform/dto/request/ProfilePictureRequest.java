package com.marketplace.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfilePictureRequest {
    @NotNull(message = "Profile picture is required")
    private MultipartFile file;

    // Optional metadata
    private String description;
}
