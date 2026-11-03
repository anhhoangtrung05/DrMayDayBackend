package com.example.drmayday.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserPrincipal lưu trữ thông tin nhận diện người dùng đã xác thực trong Spring Security Context.
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String phone;
    private final String fullName;
    private final String roleName;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String phone, String fullName, String roleName, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.phone = phone;
        this.fullName = fullName;
        this.roleName = roleName;
        this.authorities = authorities;
    }

    public UserPrincipal(Integer userId, String phone, String fullName, String roleName, Collection<? extends GrantedAuthority> authorities) {
        this(userId != null ? userId.longValue() : null, phone, fullName, roleName, authorities);
    }

    public static UserPrincipal create(Long userId, String phone, String fullName, String roleName) {
        String roleStr = roleName != null ? roleName.trim() : "patient";
        String normalized = roleStr.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        String authority = "ROLE_" + normalized;

        return new UserPrincipal(
                userId,
                phone,
                fullName,
                roleStr,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    public static UserPrincipal create(Integer userId, String phone, String fullName, String roleName) {
        return create(userId != null ? userId.longValue() : null, phone, fullName, roleName);
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

    public String getFullName() {
        return fullName;
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
