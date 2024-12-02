package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.admin.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAdminRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String password;

    private String permissions;

    private String firstName;

    private String lastName;

}