package com.marketplace.platform.service.user;

import com.marketplace.platform.domain.token.PasswordResetToken;
import com.marketplace.platform.domain.token.VerificationToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.UserResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.token.PasswordResetTokenRepository;
import com.marketplace.platform.repository.token.VerificationTokenRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.service.email.EmailService;
import com.marketplace.platform.service.storage.FileStorageService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
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
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        User savedUser = userRepository.save(user);

        // Create verification token
        VerificationToken verificationToken = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpirationMinutes))
                .build();

        verificationTokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    verificationToken.getToken(),
                    savedUser.getFirstName()
            );
        } catch (MessagingException e) {
            log.error("Failed to send verification email", e);
        }

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findValidToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        User user = verificationToken.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);

        verificationTokenRepository.markTokenAsUsed(token);

        // Cleanup any other pending tokens for this user
        verificationTokenRepository.invalidateExistingTokens(user);
    }

    @Override
    @Transactional
    public void resendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Invalidate existing tokens
        verificationTokenRepository.invalidateExistingTokens(user);

        // Create new verification token
        VerificationToken newToken = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpirationMinutes))
                .build();

        verificationTokenRepository.save(newToken);

        try {
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    newToken.getToken(),
                    user.getFirstName()
            );
        } catch (MessagingException e) {
            log.error("Failed to resend verification email", e);
            throw new BadRequestException("Failed to resend verification email");
        }
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        updateUserFields(user, request);
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    private void updateUserFields(User user, UserUpdateRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(UserSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            return userRepository.findAll(pageable).map(this::mapToUserResponse);
        }

        return userRepository.searchUsers(
                criteria.getSearchTerm(),
                criteria.getStatus() != null ? UserStatus.valueOf(criteria.getStatus()) : null,
                LocalDateTime.parse(criteria.getStartDate()),
                LocalDateTime.parse(criteria.getEndDate()),
                pageable
        ).map(this::mapToUserResponse);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = findUserById(userId);
        user.setStatus(status);
        userRepository.save(user);

        try {
            emailService.sendStatusChangeNotification(
                    user.getEmail(),
                    user.getFirstName(),
                    status.name()
            );
        } catch (MessagingException e) {
            log.error("Failed to send status change notification", e);
        }
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        return mapToUserResponse(userRepository.save(user));
    }


    @Override
    @Transactional
    public void updateProfilePicture(Long userId, ProfilePictureRequest request) {
        User user = findUserById(userId);

        if (user.getProfileImagePath() != null) {
            fileStorageService.deleteFile(user.getProfileImagePath());
        }

        String fileUrl = fileStorageService.storeFile(request.getFile());
        user.setProfileImagePath(fileUrl);
        userRepository.save(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateExistingTokens(user);

        // Create new reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenExpirationMinutes))
                .build();

        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    resetToken.getToken(),
                    user.getFirstName()
            );
        } catch (MessagingException e) {
            log.error("Failed to send password reset email", e);
            throw new BadRequestException("Failed to send password reset email");
        }
    }


    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getUsedAt() != null) {
            throw new BadRequestException("Token has already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark the token as used
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Invalidate any other tokens
        passwordResetTokenRepository.invalidateExistingTokens(user);

        try {
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());
        } catch (MessagingException e) {
            log.error("Failed to send password change notification", e);
        }
    }


    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        try {
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());
        } catch (MessagingException e) {
            log.error("Failed to send password change notification", e);
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedVerificationTokens = verificationTokenRepository.deleteExpiredTokens(now);
        int deletedResetTokens = passwordResetTokenRepository.deleteExpiredTokens(now);

        log.info("Cleaned up {} expired verification tokens and {} expired reset tokens",
                deletedVerificationTokens, deletedResetTokens);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setProfileImage(user.getProfileImagePath());
        response.setStatus(user.getStatus().name());
        response.setEmailVerified(user.getIsEmailVerified());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}

