package com.marketplace.platform.config;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.repository.admin.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String superAdminEmail;

    @Value("${app.admin.password}")
    private String superAdminPassword;

    @Value("${app.admin.firstName:System}")
    private String superAdminFirstName;

    @Value("${app.admin.lastName:Administrator}")
    private String superAdminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        initializeSuperAdmin();
    }

    private void initializeSuperAdmin() {
        if (!adminRepository.existsByEmailAndDeletedFalse(superAdminEmail)) {
            log.info("Initializing super admin account...");

            Admin superAdmin = Admin.builder()
                    .email(superAdminEmail)
                    .firstName(superAdminFirstName)
                    .lastName(superAdminLastName)
                    .passwordHash(passwordEncoder.encode(superAdminPassword))
                    .role(Role.SUPER_ADMIN)
                    .permissions("*")  // Full access
                    .isPasswordChanged(true)  // Initial password is set by configuration
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .deleted(false)
                    .build();

            adminRepository.save(superAdmin);
            log.info("Super admin account has been initialized successfully");
        } else {
            log.info("Super admin account already exists");
        }
    }
}