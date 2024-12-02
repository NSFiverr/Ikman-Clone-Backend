package com.marketplace.platform.service.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import io.jsonwebtoken.Claims;

import java.util.Optional;

public interface JwtService {
    TokenRefreshResponse refreshTokenForEntity(RefreshToken refreshToken);
    String generateAdminAccessToken(Admin admin);

    String generateRefreshToken();

    String generateUserAccessToken(User user);
    boolean validateToken(String token);
    Claims getAllClaimsFromToken(String token);
    String getEmailFromToken(String token);
    Optional<Admin> getAdminFromToken(String token);
    Optional<User> getUserFromToken(String token);
    String getRoleFromToken(String token);
}
