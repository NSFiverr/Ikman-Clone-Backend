package com.marketplace.platform.validator.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.ChangePasswordRequest;
import com.marketplace.platform.dto.request.UpdateProfileRequest;
import com.marketplace.platform.dto.request.UserRegistrationRequest;
import com.marketplace.platform.exception.ValidationException;
import com.marketplace.platform.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void validateRegistrationRequest(UserRegistrationRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email cannot be empty");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password cannot be empty");
        }
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new ValidationException("First name cannot be empty");
        }
        if (!StringUtils.hasText(request.getLastName())) {
            throw new ValidationException("Last name cannot be empty");
        }
        if (userRepository.existsByEmail(request.getEmail(), UserStatus.DELETED)) {
            throw new ValidationException("Email already registered");
        }
    }

    public void validateProfileUpdate(UpdateProfileRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email cannot be empty, Please enter your email address");
        }
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new ValidationException("First name cannot be empty, Please enter your first name");
        }
        if (!StringUtils.hasText(request.getLastName())) {
            throw new ValidationException("Last name cannot be empty, Please enter your last name");
        }
    }

    public void validateEmailUniqueness(String newEmail, User existingUser) {
        if (!existingUser.getEmail().equals(newEmail) &&
                userRepository.existsByEmail(newEmail, UserStatus.DELETED)) {
            throw new ValidationException("Email already in use");
        }
    }

    public void validatePasswordChange(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw new ValidationException("New password cannot be empty");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("New password and confirm password do not match");
        }
    }

    public void validatePasswordReset(String token, String newPassword) {
        if (!StringUtils.hasText(token)) {
            throw new ValidationException("Reset token cannot be empty");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new ValidationException("New password cannot be empty");
        }
    }

    public void validateEmailVerificationToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ValidationException("Verification token cannot be empty");
        }
    }

    public void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be empty");
        }
    }

    public void validateUserNotDeleted(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ValidationException("User is already deleted");
        }
    }

    public void validateUserEmailNotVerified(User user) {
        if (user.getIsEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }
    }
}