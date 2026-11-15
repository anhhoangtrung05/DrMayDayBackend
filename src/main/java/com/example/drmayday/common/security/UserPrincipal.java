package com.example.drmayday.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

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
        String roleStr = roleName != null ? roleName.trim() : "benh_nhan";
        String normalized = roleStr.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        String authority = "ROLE_" + normalized;

        return new UserPrincipal(
                userId,
                phone,
                roleStr,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
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
