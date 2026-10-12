package com.drmayday.common.security;

import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityUtils tiện ích trích xuất thông tin người dùng đang đăng nhập từ SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() 
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AppException(ErrorCode.USER_NOT_AUTHENTICATED);
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    public static Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static String getCurrentUserPhone() {
        return getCurrentUser().getPhone();
    }

    public static String getCurrentUserRole() {
        return getCurrentUser().getRoleName();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String targetAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetAuthority));
    }
}
