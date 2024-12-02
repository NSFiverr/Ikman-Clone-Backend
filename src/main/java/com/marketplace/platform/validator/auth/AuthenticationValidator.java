package com.marketplace.platform.validator.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.exception.AccountDeactivatedException;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationValidator {
    private final PasswordEncoder passwordEncoder;

    public void validateAuthenticationRequest(AuthenticationRequest request) {
        List<String> errors = new ArrayList<>();

        // Validate email
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("Invalid email format");
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            errors.add("Password is required");
        }

        throwIfErrors(errors);
    }

    public void validateAdminAuthentication(Admin admin, String password) {
        List<String> errors = new ArrayList<>();

        // Validate password match
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Validate admin status
        if (Boolean.TRUE.equals(admin.getDeleted())) {
            errors.add("Admin account has been deactivated");
        }

        throwIfErrors(errors);
    }


    public void validateUserAuthentication(User user, String password) {
        List<String> errors = new ArrayList<>();

        // Validate password match
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Validate user status
        if (user.getStatus() == UserStatus.DELETED) {
            errors.add("Account has been deleted");
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            errors.add("Account is not active");
        }

        // Validate email verification
        if (!user.getIsEmailVerified()) {
            errors.add("Email not verified");
        }

        throwIfErrors(errors);
    }


    public void validateLogoutRequest(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("No authenticated user found");
        }
    }

    public void validateRefreshTokenRequest(String refreshToken) {
        List<String> errors = new ArrayList<>();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            errors.add("Refresh token is required");
        }

        throwIfErrors(errors);
    }

    private void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            log.error("Authentication validation errors: {}", errors);
            if (errors.stream().anyMatch(error ->
                    error.contains("deactivated") ||
                            error.contains("deleted") ||
                            error.contains("not active") ||
                            error.contains("not verified"))) {
                throw new AccountDeactivatedException(String.join(", ", errors));
            }
            throw new BadRequestException(String.join(", ", errors));
        }
    }
}