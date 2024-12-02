package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.domain.user.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRefreshResponse {
    private String accessToken;
    private String tokenType;
    private String email;
    private String type;
    private UserData userData;
    private AdminData adminData;

    @Data
    @Builder
    public static class UserData {
        private Long userId;
        private String firstName;
        private String lastName;
        private String profileImagePath;
        private UserStatus status;
        private Boolean isEmailVerified;
    }

    @Data
    @Builder
    public static class AdminData {
        private Long adminId;
        private String firstName;
        private String lastName;
        private Role role;
        private String permissions;
    }
}