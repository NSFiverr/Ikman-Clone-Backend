package com.marketplace.platform.security;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // First try to find admin
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return buildAdminUserDetails(admin.get());
        }

        // If not admin, try to find regular user
        Optional<User> user = userRepository.findByEmail(email,  UserStatus.DELETED);
        if (user.isPresent()) {
            return buildUserUserDetails(user.get());
        }

        log.error("No user or admin found with email: {}", email);
        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    private UserDetails buildAdminUserDetails(Admin admin) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Add ROLE_ prefix as required by Spring Security
        if (admin.getRole() == Role.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // SUPER_ADMIN also has ADMIN role
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new org.springframework.security.core.userdetails.User(
                admin.getEmail(),
                admin.getPasswordHash(),
                admin.getDeleted() != null && !admin.getDeleted(), // isEnabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }

    private UserDetails buildUserUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus() == UserStatus.ACTIVE, // isEnabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                user.getStatus() != UserStatus.SUSPENDED, // accountNonLocked
                authorities
        );
    }
}