package com.marketplace.platform.service.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.response.TokenRefreshResponse;

public interface RefreshTokenService {
    TokenRefreshResponse refreshAccessToken();
    RefreshToken createRefreshTokenForAdmin(Admin admin, boolean remember);
    RefreshToken createRefreshTokenForUser(User user, boolean remember);
    void setRefreshTokenCookie(String token, boolean remember);
    void clearRefreshTokenCookie();
    RefreshToken verifyToken(String token);
    void revokeToken(String token, String revokedBy, String reason);
    void revokeAllUserTokens(User user, String revokedBy, String reason);
    void revokeAllAdminTokens(Admin admin, String revokedBy, String reason);
    void cleanupExpiredTokens();
}
