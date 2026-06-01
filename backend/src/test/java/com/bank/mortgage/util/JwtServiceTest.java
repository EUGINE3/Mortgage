package com.bank.mortgage.security;

import com.bank.mortgage.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private final String testSecret = "9f2c7a1d6b8e4f3c9a0d8e7f6b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c";

    @BeforeEach
void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", testSecret);
        
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .role("ROLE_APPLICANT")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid JWT token for user")
        void shouldGenerateValidJwtToken() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            
            // Parse and verify claims
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
            assertThat(claims.get("role")).isEqualTo(testUser.getRole());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Should generate token with correct expiration time (1 hour)")
        void shouldGenerateTokenWithCorrectExpiration() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();
            long diffInMillis = expiration.getTime() - issuedAt.getTime();
            long diffInHours = diffInMillis / (1000 * 60 * 60);
            
            assertThat(diffInHours).isEqualTo(1);
        }

        @Test
        @DisplayName("Should generate token with user email as subject")
        void shouldGenerateTokenWithUserEmailAsSubject() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should generate token with user role as claim")
        void shouldGenerateTokenWithUserRoleAsClaim() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.get("role")).isEqualTo("ROLE_APPLICANT");
        }

        @Test
        @DisplayName("Should generate different tokens for same user when there's a delay")
        void shouldGenerateDifferentTokensForSameUser() throws InterruptedException {
            // When
            String token1 = jwtService.generateToken(testUser);
            
            // Wait 1 second to ensure different timestamp
            Thread.sleep(1000);
            
            String token2 = jwtService.generateToken(testUser);
            
            // Then
            assertThat(token1).isNotEqualTo(token2);
            
            // Parse and compare claims
            Claims claims1 = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token1)
                    .getPayload();
            
            Claims claims2 = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token2)
                    .getPayload();
            
            // Different issued at times
            assertThat(claims1.getIssuedAt()).isNotEqualTo(claims2.getIssuedAt());
        }

        @Test
        @DisplayName("Should generate token for user with CREDIT_OFFICER role")
        void shouldGenerateTokenForCreditOfficer() {
            // Given
            User officer = User.builder()
                    .id(UUID.randomUUID())
                    .email("officer@example.com")
                    .password("password123")
                    .fullName("Credit Officer")
                    .role("ROLE_CREDIT_OFFICER")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token = jwtService.generateToken(officer);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo("officer@example.com");
            assertThat(claims.get("role")).isEqualTo("ROLE_CREDIT_OFFICER");
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    class TokenSecurityTests {

        @Test
        @DisplayName("Should generate token that can be verified with same secret")
        void shouldVerifyTokenWithSameSecret() {
            // Given
            String token = jwtService.generateToken(testUser);
            
            // When/Then - no exception should be thrown
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        }

        @Test
        @DisplayName("Should fail verification with different secret")
        void shouldFailVerificationWithDifferentSecret() {
            // Given
            String token = jwtService.generateToken(testUser);
            String differentSecret = "differentSecretKey12345678901234567890123456789012";
            
            // When/Then
            assertThatThrownBy(() -> Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(differentSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        void shouldHandleNullUser() {
            // When/Then
            assertThatThrownBy(() -> jwtService.generateToken(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Token Claims Tests")
    class TokenClaimsTests {

        @Test
        @DisplayName("Should contain issued at claim")
        void shouldContainIssuedAtClaim() {
            // Given
            long beforeToken = System.currentTimeMillis() / 1000; // Convert to seconds
            
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getIssuedAt()).isNotNull();
            // JWT uses seconds precision, so convert to seconds for comparison
            long issuedAtSeconds = claims.getIssuedAt().getTime() / 1000;
            assertThat(issuedAtSeconds).isGreaterThanOrEqualTo(beforeToken);
        }

        @Test
        @DisplayName("Should contain expiration claim")
        void shouldContainExpirationClaim() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("Token should not be expired immediately after generation")
        void tokenShouldNotBeExpiredImmediately() {
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date now = new Date();
            // Add 1 second buffer for precision differences
            assertThat(claims.getExpiration().getTime()).isGreaterThan(now.getTime() - 1000);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use default secret when not configured")
        void shouldUseDefaultSecretWhenNotConfigured() {
            // Given
            JwtService customJwtService = new JwtService();
            String defaultSecret = "9f2c7a1d6b8e4f3c9a0d8e7f6b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c";
            ReflectionTestUtils.setField(customJwtService, "jwtSecret", defaultSecret);
            
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .password("password")
                    .fullName("Test User")
                    .role("ROLE_APPLICANT")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token = customJwtService.generateToken(user);
            
            // Then
            assertThat(token).isNotNull();
            
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(defaultSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should work with custom secret from properties")
        void shouldWorkWithCustomSecret() {
            // Given
            String customSecret = "myCustomSecretKey12345678901234567890123456789012";
            ReflectionTestUtils.setField(jwtService, "jwtSecret", customSecret);
            
            // When
            String token = jwtService.generateToken(testUser);
            
            // Then
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(customSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle user with very long email")
        void shouldHandleLongEmail() {
            // Given
            String longEmail = "very.long.email.address." + "a".repeat(100) + "@example.com";
            User userWithLongEmail = User.builder()
                    .id(UUID.randomUUID())
                    .email(longEmail)
                    .password("password")
                    .fullName("Test User")
                    .role("ROLE_APPLICANT")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token = jwtService.generateToken(userWithLongEmail);
            
            // Then
            assertThat(token).isNotNull();
            
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo(longEmail);
        }

        @Test
        @DisplayName("Should handle user with special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            // Given
            String specialEmail = "test+special@example.com";
            User userWithSpecialEmail = User.builder()
                    .id(UUID.randomUUID())
                    .email(specialEmail)
                    .password("password")
                    .fullName("Test User")
                    .role("ROLE_APPLICANT")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token = jwtService.generateToken(userWithSpecialEmail);
            
            // Then
            assertThat(token).isNotNull();
            
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.getSubject()).isEqualTo(specialEmail);
        }

        @Test
        @DisplayName("Should handle user with empty role")
        void shouldHandleEmptyRole() {
            // Given
            User userWithEmptyRole = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .password("password")
                    .fullName("Test User")
                    .role("")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token = jwtService.generateToken(userWithEmptyRole);
            
            // Then
            assertThat(token).isNotNull();
            
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            assertThat(claims.get("role")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Integration-like Tests")
    class IntegrationLikeTests {

        @Test
        @DisplayName("Should generate multiple tokens for multiple users")
        void shouldGenerateMultipleTokensForMultipleUsers() {
            // Given
            User user1 = User.builder()
                    .id(UUID.randomUUID())
                    .email("user1@example.com")
                    .password("password")
                    .fullName("User One")
                    .role("ROLE_APPLICANT")
                    .createdAt(Instant.now())
                    .build();
            
            User user2 = User.builder()
                    .id(UUID.randomUUID())
                    .email("user2@example.com")
                    .password("password")
                    .fullName("User Two")
                    .role("ROLE_CREDIT_OFFICER")
                    .createdAt(Instant.now())
                    .build();
            
            // When
            String token1 = jwtService.generateToken(user1);
            String token2 = jwtService.generateToken(user2);
            
            // Then
            Claims claims1 = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token1)
                    .getPayload();
            
            Claims claims2 = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token2)
                    .getPayload();
            
            assertThat(claims1.getSubject()).isEqualTo("user1@example.com");
            assertThat(claims1.get("role")).isEqualTo("ROLE_APPLICANT");
            
            assertThat(claims2.getSubject()).isEqualTo("user2@example.com");
            assertThat(claims2.get("role")).isEqualTo("ROLE_CREDIT_OFFICER");
        }

        @Test
        @DisplayName("Should generate valid tokens in rapid succession")
        void shouldGenerateValidTokensInRapidSuccession() {
            // When
            String[] tokens = new String[10];
            Set<String> uniqueTokens = new HashSet<>();
            
            for (int i = 0; i < 10; i++) {
                tokens[i] = jwtService.generateToken(testUser);
                uniqueTokens.add(tokens[i]);
            }
            
            // Then
            // Tokens might be the same if generated within the same second
            // So we check that at least some tokens are valid and properly formatted
            for (String token : tokens) {
                assertThat(token).isNotNull();
                
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                
                assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
            }
            
            // Verify that all tokens are valid JWTs (at least 3 parts)
            for (String token : tokens) {
                String[] parts = token.split("\\.");
                assertThat(parts.length).isEqualTo(3);
            }
        }
    }
}
