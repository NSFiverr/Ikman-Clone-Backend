package com.marketplace.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthenticationResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdminAuthenticationResponse {
        private String accessToken;
        private String tokenType;
        private String firstName;
        private String lastName;
        private Long expiresIn;
        private Long adminId;
        private String email;
        private Role role;
        private String permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserAuthenticationResponse {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
        private Long userId;
        private String email;
        private String firstName;
        private String lastName;
        private String profileImagePath;
        private UserStatus status;
        private Boolean isEmailVerified;
    }
}