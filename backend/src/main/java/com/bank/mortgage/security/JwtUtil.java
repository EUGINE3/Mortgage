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

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret:mysecretkeymysecretkeymysecretkey12}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // -----------------------------
    // VALIDATE TOKEN
    // -----------------------------
    public boolean validateToken(String token) {

        try {
            extractClaims(token);
            return true;

        } catch (Exception e) {

            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // -----------------------------
    // BUILD SPRING AUTH OBJECT
    // -----------------------------
   public Authentication getAuthentication(String token) {

    Claims claims = extractClaims(token);

    String email = claims.getSubject();
    String role = claims.get("role", String.class);

    List<SimpleGrantedAuthority> authorities =
            role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

    return new UsernamePasswordAuthenticationToken(
            email,
            null,
            authorities
    );
}

    // -----------------------------
    // PARSE CLAIMS
    // -----------------------------
    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
