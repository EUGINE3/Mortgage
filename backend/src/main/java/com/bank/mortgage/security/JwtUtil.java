package com.bank.mortgage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret:secret-key-for-jwt-token-signing}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        
        List<SimpleGrantedAuthority> authorities;
        if (role != null) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        } else {
            authorities = Collections.emptyList();
        }

        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}
