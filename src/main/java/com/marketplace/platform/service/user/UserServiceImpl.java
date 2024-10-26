package com.marketplace.platform.service.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.domain.user.VerificationToken;
import com.marketplace.platform.domain.user.PasswordResetToken;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.UserResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.repository.user.VerificationTokenRepository;
import com.marketplace.platform.repository.user.PasswordResetTokenRepository;
import com.marketplace.platform.service.email.EmailService;
import com.marketplace.platform.service.storage.FileStorageService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(savedUser);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        try {
            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    verificationToken.getToken(),
                    savedUser.getFirstName()
            );
        } catch (MessagingException e) {
            // Log the error but don't stop the registration process
            // You might want to implement a retry mechanism
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
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);

        if (verificationToken == null) {
            throw new BadRequestException("Invalid verification token");
        }

        if (verificationToken.isUsed()) {
            throw new BadRequestException("Token already used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
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

        if (user.getProfileImage() != null) {
            fileStorageService.deleteFile(user.getProfileImage());
        }

        String fileUrl = fileStorageService.storeFile(request.getFile());
        user.setProfileImage(fileUrl);
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

        // Invalidate any existing reset tokens
        passwordResetTokenRepository.invalidateExistingTokens(user);

        // Create new reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
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
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadRequestException("Token is invalid or expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

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

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setProfileImage(user.getProfileImage());
        response.setStatus(user.getStatus().name());
        response.setEmailVerified(user.isEmailVerified());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}

