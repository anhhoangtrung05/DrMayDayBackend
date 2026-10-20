package com.drmayday.authrbac.security;

import com.drmayday.common.exception.AppException;
import com.drmayday.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Claims claims = jwtTokenProvider.parseAndValidateClaims(jwt);
                Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
                String phone = jwtTokenProvider.getPhoneFromClaims(claims);
                String role = jwtTokenProvider.getRoleFromClaims(claims);

                UserPrincipal principal = UserPrincipal.create(userId, phone, phone, role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AppException ex) {
            log.warn("Lỗi xác thực JWT [code={}, message={}]: {}", ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
            request.setAttribute("AUTH_EXCEPTION", ex);
        } catch (Exception ex) {
            log.warn("Không thể thiết lập xác thực người dùng trong Security Context: {}", ex.getMessage());
            request.setAttribute("AUTH_EXCEPTION", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken)) {
            String trimmed = bearerToken.trim();
            if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return trimmed.substring(7).trim();
            }
            // Hỗ trợ trường hợp client truyền trực tiếp chuỗi JWT (bắt đầu bằng ey... và có 2 dấu chấm)
            if (trimmed.startsWith("ey") && trimmed.chars().filter(ch -> ch == '.').count() == 2) {
                return trimmed;
            }
        }
        return null;
    }
}
