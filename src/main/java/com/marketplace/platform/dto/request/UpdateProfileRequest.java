package com.marketplace.platform.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UpdateProfileRequest {
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;
    private String address;
    private String bio;
}
