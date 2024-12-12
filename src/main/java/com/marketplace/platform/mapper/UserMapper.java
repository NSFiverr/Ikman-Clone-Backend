package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.UserRegistrationRequest;
import com.marketplace.platform.dto.response.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private final PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User toEntity(UserRegistrationRequest request) {
        return User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isEmailVerified(false)
                .displayPhone(request.getDisplayPhone())
                .displayEmail(request.getDisplayEmail())
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImagePath())
                .status(user.getStatus().name())
                .isEmailVerified(user.getIsEmailVerified())
                .displayPhone(user.getDisplayPhone())
                .displayEmail(user.getDisplayEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}