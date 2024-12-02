package com.marketplace.platform.mapper;

import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.response.AuthenticationResponse.AdminAuthenticationResponse;
import com.marketplace.platform.dto.response.AuthenticationResponse.UserAuthenticationResponse;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationMapper {
    private final JwtProperties jwtProperties;

    public AdminAuthenticationResponse toAdminResponse(Admin admin, String accessToken) {
        return AdminAuthenticationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenValidity() / 1000)
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .permissions(admin.getPermissions())
                .build();
    }

    public UserAuthenticationResponse toUserResponse(User user, String accessToken) {
        return UserAuthenticationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenValidity() / 1000)
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImagePath(user.getProfileImagePath())
                .status(user.getStatus())
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }

    public TokenRefreshResponse toRefreshResponse(User user, String accessToken) {
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .type("USER")
                .userData(TokenRefreshResponse.UserData.builder()
                        .userId(user.getUserId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .profileImagePath(user.getProfileImagePath())
                        .status(user.getStatus())
                        .isEmailVerified(user.getIsEmailVerified())
                        .build())
                .build();
    }

    public TokenRefreshResponse toRefreshResponse(Admin admin, String accessToken) {
        String type = (admin.getRole() == Role.SUPER_ADMIN ) ? "SUPER_ADMIN" : "ADMIN";
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .email(admin.getEmail())
                .type(type)
                .adminData(TokenRefreshResponse.AdminData.builder()
                        .adminId(admin.getAdminId())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .role(admin.getRole())
                        .permissions(admin.getPermissions())
                        .build())
                .build();
    }
}