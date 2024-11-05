package com.marketplace.platform.service.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    // Essential CRUD operations
    UserResponse registerUser(UserRegistrationRequest request);
    UserResponse getUserById(Long userId);
    UserResponse updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    // Authentication related
    UserResponse getUserByEmail(String email);
    boolean isEmailTaken(String email);
    void verifyEmail(String token);

    void resendVerificationToken(String email);

    // Password management
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(Long userId, ChangePasswordRequest request);

    // User search and filtering
    Page<UserResponse> getAllUsers(UserSearchCriteria criteria, Pageable pageable);

    // Status management
    void updateUserStatus(Long userId, UserStatus status);

    // Profile management
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void updateProfilePicture(Long userId, ProfilePictureRequest request);
}
