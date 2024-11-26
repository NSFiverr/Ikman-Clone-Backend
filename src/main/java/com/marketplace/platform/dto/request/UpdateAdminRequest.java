package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.admin.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAdminRequest {


    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "Password must be at least 8 characters long and contain both letters and numbers")
    private String password;

    private Role role;
    private String permissions;

}