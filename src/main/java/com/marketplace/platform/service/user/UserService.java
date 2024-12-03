package com.marketplace.platform.service.user;

import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

import java.util.Optional;

public interface UserService {
    // Essential CRUD operations
    UserResponse registerUser(@Valid UserRegistrationRequest request);
    UserResponse getUserById(Long userId);
    UserResponse updateUser(Long userId, @Valid UserUpdateRequest request);
    void softDeleteUser(Long userId);

    // Authentication related
    Optional<UserResponse> getUserByEmail(String email);
    boolean isEmailTaken(String email);
    void verifyEmail(String token);
    void resendVerificationToken(String email);

    // Password management
    void changePassword(Long userId, @Valid ChangePasswordRequest request);

    // User search and filtering
    Page<UserResponse> searchUsers(@Valid UserSearchCriteria criteria, Pageable pageable);

    // Status management
    void updateUserStatus(Long userId, UserStatus status);

    // Profile management
    UserResponse updateProfile(Long userId, @Valid UpdateProfileRequest request);
    void updateProfilePicture(Long userId, @Valid ProfilePictureRequest request);

    // Utility methods
    Page<UserResponse> getInactiveUsers(int inactiveDays, Pageable pageable);
    Page<UserResponse> getUsersWithUnreadNotifications(Pageable pageable);
}