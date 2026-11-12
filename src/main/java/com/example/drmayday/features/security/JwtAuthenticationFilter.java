package com.example.drmayday.features.security;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.security.UserPrincipal;
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
                Long userId = Long.valueOf(claims.getSubject());
                String phone = claims.get("phone", String.class);
                String role = claims.get("role", String.class);

                UserPrincipal principal = UserPrincipal.create(userId, phone, role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AppException ex) {
            log.warn("JWT authentication failed [code={}, message={}]: {}", ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
            request.setAttribute("AUTH_EXCEPTION", ex);
        } catch (Exception ex) {
            log.warn("Failed to set user authentication in the security context: {}", ex.getMessage());
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
            // Support clients that send a raw JWT directly (starts with "ey" and contains two periods).
            if (trimmed.startsWith("ey") && trimmed.chars().filter(ch -> ch == '.').count() == 2) {
                return trimmed;
            }
        }
        return null;
    }
}
