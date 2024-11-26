package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "Please enter a valid email address")
    private String email;

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please enter a valid phone number")
    private String phone;

    private UserStatus status;

    @Size(max = 255, message = "Profile image path is too long")
    private String profileImagePath;

    private Boolean isEmailVerified;

    // Custom validation method if needed
    public boolean hasUpdates() {
        return email != null ||
                firstName != null ||
                lastName != null ||
                phone != null ||
                status != null ||
                profileImagePath != null ||
                isEmailVerified != null;
    }
}