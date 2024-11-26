package com.marketplace.platform.security;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.repository.admin.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (admin.getRole() == Role.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // SUPER_ADMIN also has ADMIN role
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(
                admin.getEmail(),
                admin.getPasswordHash(),
                authorities
        );
    }
}