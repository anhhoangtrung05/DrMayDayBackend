package com.example.drmayday.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores the identity information of an authenticated user in the Spring Security context.
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String phone;
    private final String roleName;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String phone, String roleName, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.phone = phone;
        this.roleName = roleName;
        this.authorities = authorities;
    }

    public static UserPrincipal create(Long userId, String phone, String roleName) {
        String roleStr = roleName != null && !roleName.isBlank() ? roleName.trim() : "benh_nhan";
        String normalized = roleStr.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        // Map both DB Vietnamese role names (benh_nhan, bac_si, ...) and English @PreAuthorize aliases (PATIENT, DOCTOR, ...)
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));

        switch (normalized) {
            case "BENH_NHAN", "PATIENT" -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_BENH_NHAN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_PATIENT"));
            }
            case "BAC_SI", "DOCTOR" -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_BAC_SI"));
                authorities.add(new SimpleGrantedAuthority("ROLE_DOCTOR"));
            }
            case "DIEU_DUONG", "NURSE" -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_DIEU_DUONG"));
                authorities.add(new SimpleGrantedAuthority("ROLE_NURSE"));
            }
            case "QUAN_TRI", "ADMIN" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            default -> {
                // Keep exact normalized authority for any other custom role
            }
        }

        return new UserPrincipal(
                userId,
                phone,
                roleStr,
                Collections.unmodifiableSet(authorities)
        );
    }

    public Long getUserId() {
        return userId;
    }

    public Long getUserIdAsLong() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
