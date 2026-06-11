package com.bank.mortgage.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip JWT authentication for public endpoints
        if (shouldSkipJwtAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("Processing request to: {}", request.getRequestURI());
        log.debug("Authorization header: {}", request.getHeader("Authorization"));

        String token = extractToken(request);
        log.debug("Token extracted: {}", token != null ? "present (length: " + token.length() + ")" : "null");

        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Authentication authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT authenticated user: {} for request to {}",
                        authentication.getName(), request.getRequestURI());
            } catch (Exception e) {
                log.error("JWT authentication error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                sendAuthenticationError(response, "Invalid token: " + e.getMessage());
                return;
            }
        } else if (token != null) {
            // Token exists but is invalid
            log.warn("Invalid JWT token for request to {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
            sendAuthenticationError(response, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipJwtAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/home") ||
                path.equals("/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/health");
    }

    private void sendAuthenticationError(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
