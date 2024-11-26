package com.marketplace.platform.service.user;

import com.marketplace.platform.domain.token.PasswordResetToken;
import com.marketplace.platform.domain.token.VerificationToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.UserResponse;
import com.marketplace.platform.exception.*;
import com.marketplace.platform.repository.token.PasswordResetTokenRepository;
import com.marketplace.platform.repository.token.VerificationTokenRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.service.email.EmailService;
import com.marketplace.platform.service.storage.FileStorageService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = {"users"})
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @Value("${app.token.verification.expiration-minutes}")
    private int verificationTokenExpirationMinutes;

    @Value("${app.token.password-reset.expiration-minutes}")
    private int passwordResetTokenExpirationMinutes;


    @Override
    public UserResponse getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        return mapToUserResponse(findUserById(userId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"users"}, allEntries = true)
    public UserResponse registerUser(@Valid UserRegistrationRequest request) {
        log.debug("Attempting to register new user with email: {}", request.getEmail());

        validateRegistrationRequest(request);

        try {
            User user = createUserFromRequest(request);
            User savedUser = userRepository.save(user);
            VerificationToken verificationToken = createVerificationToken(savedUser);

            sendVerificationEmail(savedUser, verificationToken);

            log.info("Successfully registered new user with ID: {}", savedUser.getUserId());
            return mapToUserResponse(savedUser);

        } catch (Exception e) {
            log.error("Failed to register user with email: {}", request.getEmail(), e);
            throw new RegistrationException("Failed to register user", e);
        }
    }

    @Override
    @Cacheable(cacheNames = "users", key = "#email", unless = "#result == null")
    public Optional<UserResponse> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be empty");
        }

        try {
            return userRepository.findByEmail(email, UserStatus.DELETED)
                    .map(this::mapToUserResponse);
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", email, e);
            throw new ServiceException("Error retrieving user", e);
        }
    }

    @Override
    @Transactional
    @Retryable(
            value = { EmailException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void verifyEmail(String token) {
        log.debug("Attempting to verify email with token: {}", token);

        if (!StringUtils.hasText(token)) {
            throw new ValidationException("Verification token cannot be empty");
        }

        try {
            VerificationToken verificationToken = verificationTokenRepository.findValidToken(token)
                    .orElseThrow(() -> new TokenException("Invalid or expired verification token"));

            User user = verificationToken.getUser();
            if (user.getIsEmailVerified()) {
                throw new ValidationException("Email is already verified");
            }

            user.setIsEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            verificationTokenRepository.markTokenAsUsed(token);
            verificationTokenRepository.invalidateExistingTokens(user);

            sendWelcomeEmail(user);

            log.info("Successfully verified email for user ID: {}", user.getUserId());

        } catch (TokenException | ValidationException e) {
            log.warn("Email verification failed for token: {}", token, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during email verification for token: {}", token, e);
            throw new ServiceException("Failed to verify email", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"users"}, allEntries = true)
    public UserResponse updateProfile(Long userId, @Valid UpdateProfileRequest request) {
        log.debug("Updating profile for user ID: {}", userId);

        validateProfileUpdateRequest(request);

        try {
            User user = findUserById(userId);
            validateEmailUniqueness(request.getEmail(), user);

            updateUserProfile(user, request);
            User updatedUser = userRepository.save(user);

            log.info("Successfully updated profile for user ID: {}", userId);
            return mapToUserResponse(updatedUser);

        } catch (ValidationException e) {
            log.warn("Profile update validation failed for user ID: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to update profile for user ID: {}", userId, e);
            throw new ServiceException("Failed to update user profile", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"users"}, allEntries = true)
    public UserResponse updateUser(Long userId, @Valid UserUpdateRequest request) {
        log.debug("Updating user with ID: {}", userId);

        try {
            User user = findUserById(userId);

            // Validate and check email uniqueness if email is being updated
            if (StringUtils.hasText(request.getEmail())) {
                validateEmailUniqueness(request.getEmail(), user);
            }

            // Update user fields
            if (StringUtils.hasText(request.getEmail())) {
                user.setEmail(request.getEmail());
            }
            if (StringUtils.hasText(request.getFirstName())) {
                user.setFirstName(request.getFirstName());
            }
            if (StringUtils.hasText(request.getLastName())) {
                user.setLastName(request.getLastName());
            }
            if (StringUtils.hasText(request.getPhone())) {
                user.setPhone(request.getPhone());
            }
            if (request.getStatus() != null) {
                user.setStatus(request.getStatus());
            }

            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            log.info("Successfully updated user with ID: {}", userId);
            return mapToUserResponse(updatedUser);

        } catch (ResourceNotFoundException e) {
            log.warn("Update failed - User not found with ID: {}", userId);
            throw e;
        } catch (ValidationException e) {
            log.warn("Update failed - Validation error for user ID: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to update user with ID: {}", userId, e);
            throw new ServiceException("Failed to update user", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"users"}, allEntries = true)
    public void softDeleteUser(Long userId) {
        log.debug("Attempting to soft delete user with ID: {}", userId);

        try {
            User user = findUserById(userId);

            if (user.getStatus() == UserStatus.DELETED) {
                throw new ValidationException("User is already deleted");
            }

            user.setStatus(UserStatus.DELETED);
            // Append timestamp to email to allow reuse of the email in future
            user.setEmail(user.getEmail() + "_deleted_" + System.currentTimeMillis());
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);


            verificationTokenRepository.invalidateExistingTokens(user);
            passwordResetTokenRepository.invalidateExistingTokens(user);

            if (StringUtils.hasText(user.getProfileImagePath())) {
                fileStorageService.deleteFile(user.getProfileImagePath());
            }

            log.info("Successfully soft deleted user with ID: {}", userId);

        } catch (ResourceNotFoundException e) {
            log.warn("Soft delete failed - User not found with ID: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to soft delete user with ID: {}", userId, e);
            throw new ServiceException("Failed to delete user", e);
        }
    }


    @Override
    public boolean isEmailTaken(String email) {
        log.debug("Checking if email is taken: {}", email);
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be empty");
        }
        return userRepository.existsByEmail(email, UserStatus.DELETED);
    }

    @Override
    @Transactional
    @Retryable(
            value = { EmailException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void resendVerificationToken(String email) {
        log.debug("Resending verification token for email: {}", email);

        User user = userRepository.findByEmail(email, UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }

        // Invalidate existing tokens
        verificationTokenRepository.invalidateExistingTokens(user);

        // Create new token
        VerificationToken newToken = createVerificationToken(user);

        sendVerificationEmail(user, newToken);

        log.info("Successfully resent verification token for user: {}", email);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        log.debug("Initiating password reset for email: {}", email);

        User user = userRepository.findByEmail(email, UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateExistingTokens(user);

        // Create new reset token
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenExpirationMinutes))
                .build();

        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    tokenString,
                    user.getFirstName()
            );
            log.info("Successfully sent password reset email to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new EmailException("Failed to send password reset email");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.debug("Resetting password with token");

        if (!StringUtils.hasText(newPassword)) {
            throw new ValidationException("New password cannot be empty");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new TokenException("Invalid or expired password reset token"));

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all reset tokens
        passwordResetTokenRepository.invalidateExistingTokens(user);

        try {
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());
            log.info("Successfully reset password for user: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send password change confirmation email to: {}", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.debug("Changing password for user ID: {}", userId);

        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("New password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        try {
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());
            log.info("Successfully changed password for user ID: {}", userId);
        } catch (MessagingException e) {
            log.error("Failed to send password change confirmation email to: {}", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        log.debug("Updating status to {} for user ID: {}", status, userId);

        User user = findUserById(userId);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        try {
            emailService.sendStatusChangeNotification(user.getEmail(), user.getFirstName(), status.name());
            log.info("Successfully updated status to {} for user ID: {}", status, userId);
        } catch (MessagingException e) {
            log.error("Failed to send status change notification to: {}", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void updateProfilePicture(Long userId, ProfilePictureRequest request) {
        log.debug("Updating profile picture for user ID: {}", userId);

        User user = findUserById(userId);

        // Delete existing profile picture if exists
        if (StringUtils.hasText(user.getProfileImagePath())) {
            fileStorageService.deleteFile(user.getProfileImagePath());
        }

        String newImagePath = fileStorageService.storeFile(request.getFile());
        user.setProfileImagePath(newImagePath);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Successfully updated profile picture for user ID: {}", userId);
    }

    @Override
    public Page<UserResponse> getInactiveUsers(int inactiveDays, Pageable pageable) {
        log.debug("Fetching inactive users for {} days. Page: {}, Size: {}",
                inactiveDays, pageable.getPageNumber(), pageable.getPageSize());

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(inactiveDays);
            Page<User> usersPage = userRepository.findByLastLoginAtBefore(cutoffDate, UserStatus.DELETED, pageable);
            return usersPage.map(this::mapToUserResponse);
        } catch (Exception e) {
            log.error("Error fetching inactive users", e);
            throw new ServiceException("Failed to fetch inactive users", e);
        }
    }

    @Override
    public Page<UserResponse> getUsersWithUnreadNotifications(Pageable pageable) {
        log.debug("Fetching users with unread notifications. Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<User> usersPage = userRepository.findUsersWithUnreadNotifications(UserStatus.DELETED, pageable);
            return usersPage.map(this::mapToUserResponse);
        } catch (Exception e) {
            log.error("Error fetching users with unread notifications", e);
            throw new ServiceException("Failed to fetch users with unread notifications", e);
        }
    }


    @Override
    public Page<UserResponse> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching users with criteria: {}", criteria);

        try {
            return userRepository.searchUsers(
                    criteria.getSearchTerm(),
                    criteria.getStatus() != null ? UserStatus.valueOf(criteria.getStatus()) : null,
                    criteria.getStartDate() != null ? LocalDateTime.parse(criteria.getStartDate()) : null,
                    criteria.getEndDate() != null ? LocalDateTime.parse(criteria.getEndDate()) : null,
                    UserStatus.DELETED,
                    pageable
            ).map(this::mapToUserResponse);
        } catch (IllegalArgumentException e) {
            log.error("Invalid search criteria: {}", criteria, e);
            throw new ValidationException("Invalid search criteria provided");
        } catch (Exception e) {
            log.error("Error searching users with criteria: {}", criteria, e);
            throw new ServiceException("Error searching users", e);
        }
    }

    // Private helper methods
    private void validateRegistrationRequest(UserRegistrationRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email cannot be empty");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password cannot be empty");
        }
        if (userRepository.existsByEmail(request.getEmail(), UserStatus.DELETED)) {
            throw new ValidationException("Email already registered");
        }
    }

    private User createUserFromRequest(UserRegistrationRequest request) {
        return User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .isEmailVerified(false)
                .build();
    }

    private VerificationToken createVerificationToken(User user) {
        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpirationMinutes))
                .build();
        return verificationTokenRepository.save(token);
    }

    @Retryable(
            value = { EmailException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    private void sendVerificationEmail(User user, VerificationToken token) {
        try {
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    token.getToken(),
                    user.getFirstName()
            );
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new EmailException("Failed to send verification email", e);
        }
    }

    private void validateEmailUniqueness(String newEmail, User existingUser) {
        if (!existingUser.getEmail().equals(newEmail) &&
                userRepository.existsByEmail(newEmail, UserStatus.DELETED)) {
            throw new ValidationException("Email already in use");
        }
    }

    private void updateUserProfile(User user, UpdateProfileRequest request) {
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private void validateProfileUpdateRequest(UpdateProfileRequest request) {
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

    @Retryable(
            value = { EmailException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    private void sendWelcomeEmail(User user) {
        try {
            emailService.sendWelcomeEmail(
                    user.getEmail(),
                    user.getFirstName()
            );
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            throw new EmailException("Failed to send welcome email");
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImagePath())
                .status(user.getStatus().name())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}