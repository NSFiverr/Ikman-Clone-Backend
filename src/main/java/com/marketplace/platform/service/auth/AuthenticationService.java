package com.marketplace.platform.service.auth;

import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.dto.response.AuthenticationResponse.AdminAuthenticationResponse;
import com.marketplace.platform.dto.response.AuthenticationResponse.UserAuthenticationResponse;
import com.marketplace.platform.exception.AccountDeactivatedException;
import com.marketplace.platform.exception.InvalidCredentialsException;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.user.UserRepository;
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
public class AuthenticationService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public Object authenticate(AuthenticationRequest request) {
        try {
            // First try to authenticate as admin
            Optional<Admin> adminOpt = adminRepository.findByEmail(request.getEmail());
            if (adminOpt.isPresent()) {
                return authenticateAdmin(adminOpt.get(), request.getPassword());
            }

            // If not an admin, try to authenticate as user
            User user = userRepository.findByEmail(request.getEmail(), UserStatus.DELETED)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            return authenticateUser(user, request.getPassword());

        } catch (InvalidCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw e;
        }
    }

    private AdminAuthenticationResponse authenticateAdmin(Admin admin, String password) {
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (Boolean.TRUE.equals(admin.getDeleted())) {
            throw new AccountDeactivatedException("Admin account has been deactivated");
        }

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
        RefreshToken refreshToken = refreshTokenService.createRefreshTokenForAdmin(admin);

        log.info("Admin authenticated successfully: {}", admin.getEmail());

        return AdminAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenValidity() / 1000)
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .adminType(admin.getAdminType())
                .permissions(admin.getPermissions())
                .build();
    }

    private UserAuthenticationResponse authenticateUser(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new AccountDeactivatedException("Account has been deleted");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDeactivatedException("Account is not active");
        }

        if (!user.getIsEmailVerified()) {
            throw new AccountDeactivatedException("Email not verified");
        }

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
        RefreshToken refreshToken = refreshTokenService.createRefreshTokenForUser(user);

        log.info("User authenticated successfully: {}", user.getEmail());

        return UserAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
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

    @Transactional
    public void logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Try to find and logout admin first
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            refreshTokenService.revokeAllAdminTokens(
                    admin,
                    admin.getEmail(),
                    "User initiated logout"
            );
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
            log.info("User logged out successfully: {}", email);
        }
    }
}