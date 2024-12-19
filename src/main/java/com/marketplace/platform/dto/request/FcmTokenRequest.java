package com.marketplace.platform.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class FcmTokenRequest {
    @NotBlank(message = "FCM token cannot be empty")
    private String token;
}