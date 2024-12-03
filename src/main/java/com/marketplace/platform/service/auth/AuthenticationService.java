package com.marketplace.platform.service.auth;

import com.marketplace.platform.dto.request.AuthenticationRequest;

public interface AuthenticationService {
    Object authenticate(AuthenticationRequest request);
    void logout();

    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
}
