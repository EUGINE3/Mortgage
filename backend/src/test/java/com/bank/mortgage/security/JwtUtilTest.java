package com.bank.mortgage.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    
    private static final String TEST_SECRET = "9f2c7a1d6b8e4f3c9a0d8e7f6b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE = "ADMIN";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
    }

    private String generateValidToken(String username, String role, long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);
        
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    private String generateValidToken(String username, String role) {
        return generateValidToken(username, role, 3600000);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = generateValidToken(TEST_USERNAME, TEST_ROLE);
        boolean isValid = jwtUtil.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        String token = generateValidToken(TEST_USERNAME, TEST_ROLE, -3600000);
        boolean isValid = jwtUtil.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        String malformedToken = "malformed.token.string";
        boolean isValid = jwtUtil.validateToken(malformedToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithInvalidSignature_ShouldReturnFalse() {
        String differentSecret = "this-is-a-completely-different-secret-key-32bytes!";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));
        
        String token = Jwts.builder()
                .subject(TEST_USERNAME)
                .claim("role", TEST_ROLE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        boolean isValid = jwtUtil.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validateToken_WithInvalidTokens_ShouldReturnFalse(String invalidToken) {
        boolean isValid = jwtUtil.validateToken(invalidToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void getAuthentication_WithValidToken_ShouldReturnAuthentication() {
        String token = generateValidToken(TEST_USERNAME, TEST_ROLE);
        Authentication authentication = jwtUtil.getAuthentication(token);
        
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(TEST_USERNAME);
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_" + TEST_ROLE);
    }

    @Test
    void getAuthentication_WithExpiredToken_ShouldThrowException() {
        String token = generateValidToken(TEST_USERNAME, TEST_ROLE, -1000);
        
        assertThatThrownBy(() -> jwtUtil.getAuthentication(token))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void getAuthentication_WithMalformedToken_ShouldThrowException() {
        String malformedToken = "not-a-valid-jwt-token";
        
        assertThatThrownBy(() -> jwtUtil.getAuthentication(malformedToken))
            .isInstanceOf(MalformedJwtException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "USER, ROLE_USER",
        "ADMIN, ROLE_ADMIN",
        "MANAGER, ROLE_MANAGER"
    })
    void getAuthentication_WithDifferentRoles_ShouldReturnCorrectAuthorities(String role, String expectedAuthority) {
        String token = generateValidToken(TEST_USERNAME, role);
        Authentication authentication = jwtUtil.getAuthentication(token);
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo(expectedAuthority);
    }

    @Test
    void getAuthentication_WithTokenHavingNoRole_ShouldReturnEmptyAuthorities() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
        
        Authentication authentication = jwtUtil.getAuthentication(token);
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void validateToken_WithNullToken_ShouldReturnFalse() {
        boolean isValid = jwtUtil.validateToken(null);
        assertThat(isValid).isFalse();
    }
    
    @Test
    void getAuthentication_ShouldReturnUsernamePasswordAuthenticationToken() {
        String token = generateValidToken(TEST_USERNAME, TEST_ROLE);
        Authentication authentication = jwtUtil.getAuthentication(token);
        
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
    }
}
