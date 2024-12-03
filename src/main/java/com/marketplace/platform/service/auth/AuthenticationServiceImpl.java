package com.marketplace.platform.service.auth;

import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.AccountType;
import com.marketplace.platform.domain.token.PasswordResetToken;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.dto.response.AuthenticationResponse.AdminAuthenticationResponse;
import com.marketplace.platform.dto.response.AuthenticationResponse.UserAuthenticationResponse;
import com.marketplace.platform.exception.EmailException;
import com.marketplace.platform.exception.InvalidCredentialsException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.exception.TokenException;
import com.marketplace.platform.mapper.AuthenticationMapper;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.token.PasswordResetTokenRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.service.email.EmailService;
import com.marketplace.platform.validator.auth.AuthenticationValidator;
import com.marketplace.platform.validator.user.UserValidator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AuthenticationValidator authValidator;
    private final AuthenticationMapper authMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserValidator userValidator;
    private final EmailService emailService;



    @Value("${app.token.password-reset.expiration-minutes}")
    private int passwordResetTokenExpirationMinutes;


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
    public void initiatePasswordReset(String email) {
        log.debug("Initiating password reset for email: {}", email);

        try {
            // First try to find admin
            Optional<Admin> adminOpt = adminRepository.findByEmail(email);
            if (adminOpt.isPresent()) {
                handlePasswordReset(adminOpt.get(), email);
                return;
            }

            // If not admin, try to find user
            User user = userRepository.findByEmail(email, UserStatus.DELETED)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            handlePasswordReset(user, email);

        } catch (ResourceNotFoundException e) {
            // Log attempt but throw same error to avoid email enumeration
            log.warn("Password reset attempted for non-existent email: {}", email);
            throw e;
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.debug("Resetting password with token");
        userValidator.validatePasswordReset(token, newPassword);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new TokenException("Invalid or expired password reset token"));

        if (resetToken.isUsed()) {
            throw new TokenException("Token has already been used");
        }

        // Handle password reset based on account type
        if (resetToken.getAdmin() != null) {
            Admin admin = resetToken.getAdmin();
            admin.setPasswordHash(passwordEncoder.encode(newPassword));
            adminRepository.save(admin);

            try {
                emailService.sendPasswordChangeNotification(admin.getEmail(), admin.getFirstName());
                log.info("Successfully reset password for admin: {}", admin.getEmail());
            } catch (MessagingException e) {
                log.error("Failed to send password change confirmation email to admin: {}", admin.getEmail(), e);
            }

            passwordResetTokenRepository.invalidateExistingAdminTokens(admin);
        } else {
            User user = resetToken.getUser();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            try {
                emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());
                log.info("Successfully reset password for user: {}", user.getEmail());
            } catch (MessagingException e) {
                log.error("Failed to send password change confirmation email to user: {}", user.getEmail(), e);
            }

            passwordResetTokenRepository.invalidateExistingUserTokens(user);
        }

        // Mark the current token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);
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

    private void handlePasswordReset(Object account, String email) {
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenString)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenExpirationMinutes))
                .accountType(account instanceof Admin ? AccountType.ADMIN : AccountType.USER)
                .build();

        if (account instanceof Admin admin) {
            resetToken.setAdmin(admin);
            passwordResetTokenRepository.invalidateExistingAdminTokens(admin);

            try {
                emailService.sendPasswordResetEmail(
                        admin.getEmail(),
                        tokenString,
                        admin.getFirstName()
                );
                log.info("Successfully sent password reset email to admin: {}", admin.getEmail());
            } catch (MessagingException e) {
                log.error("Failed to send password reset email to admin: {}", admin.getEmail(), e);
                throw new EmailException("Failed to send password reset email");
            }
        } else if (account instanceof User user) {
            resetToken.setUser(user);
            passwordResetTokenRepository.invalidateExistingUserTokens(user);

            try {
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        tokenString,
                        user.getFirstName()
                );
                log.info("Successfully sent password reset email to user: {}", user.getEmail());
            } catch (MessagingException e) {
                log.error("Failed to send password reset email to user: {}", user.getEmail(), e);
                throw new EmailException("Failed to send password reset email");
            }
        } else {
            throw new IllegalArgumentException("Unsupported account type");
        }

        passwordResetTokenRepository.save(resetToken);
    }


}