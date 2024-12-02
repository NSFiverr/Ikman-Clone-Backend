package com.marketplace.platform.service.auth;

import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.dto.response.AuthenticationResponse.AdminAuthenticationResponse;
import com.marketplace.platform.dto.response.AuthenticationResponse.UserAuthenticationResponse;
import com.marketplace.platform.exception.InvalidCredentialsException;
import com.marketplace.platform.mapper.AuthenticationMapper;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.validator.auth.AuthenticationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AuthenticationValidator authValidator;
    private final AuthenticationMapper authMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public Object authenticate(AuthenticationRequest request) {
        try {
            authValidator.validateAuthenticationRequest(request);

            // First try to authenticate as admin
            Optional<Admin> adminOpt = adminRepository.findByEmail(request.getEmail());
            if (adminOpt.isPresent()) {
                return authenticateAdmin(adminOpt.get(), request.getPassword(), request.isRemember());
            }

            // If not an admin, try to authenticate as user
            User user = userRepository.findByEmail(request.getEmail(), UserStatus.DELETED)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            return authenticateUser(user, request.getPassword(), request.isRemember());

        } catch (InvalidCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw e;
        }
    }

    @Override
    @Transactional
    public void logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authValidator.validateLogoutRequest(email);

        // Try to find and logout admin first
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            refreshTokenService.revokeAllAdminTokens(
                    admin,
                    admin.getEmail(),
                    "User initiated logout"
            );
            refreshTokenService.clearRefreshTokenCookie();
            log.info("Admin logged out successfully: {}", email);
            return;
        }

        // If not admin, try to find and logout user
        Optional<User> userOpt = userRepository.findByEmail(email, UserStatus.DELETED);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            refreshTokenService.revokeAllUserTokens(
                    user,
                    user.getEmail(),
                    "User initiated logout"
            );
            refreshTokenService.clearRefreshTokenCookie();
            log.info("User logged out successfully: {}", email);
        }
    }

    private AdminAuthenticationResponse authenticateAdmin(Admin admin, String password, boolean remember) {
        authValidator.validateAdminAuthentication(admin, password);

        // Update last access time
        admin.setLastAccessAt(LocalDateTime.now());
        adminRepository.updateLastAccessAt(admin.getAdminId(), LocalDateTime.now());

        // Revoke any existing refresh tokens for security
        refreshTokenService.revokeAllAdminTokens(
                admin,
                "SYSTEM",
                "New login initiated"
        );

        // Generate new tokens
        String accessToken = jwtService.generateAdminAccessToken(admin);
        RefreshToken refreshToken = refreshTokenService.createRefreshTokenForAdmin(admin, remember);

        log.info("Admin authenticated successfully: {}", admin.getEmail());

        return authMapper.toAdminResponse(admin, accessToken);
    }

    private UserAuthenticationResponse authenticateUser(User user, String password, boolean remember) {

        authValidator.validateUserAuthentication(user, password);

        // Update last login time using repository method
        userRepository.updateLastLogin(user.getUserId(), UserStatus.DELETED);

        // Revoke any existing refresh tokens for security
        refreshTokenService.revokeAllUserTokens(
                user,
                "SYSTEM",
                "New login initiated"
        );

        // Generate new tokens
        String accessToken = jwtService.generateUserAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshTokenForUser(user, remember);

        log.info("User authenticated successfully: {}", user.getEmail());

        return authMapper.toUserResponse(user, accessToken);
    }


}