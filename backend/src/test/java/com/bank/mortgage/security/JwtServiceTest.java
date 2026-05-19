package com.bank.mortgage.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        String token = jwtService.generateToken(testEmail);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void extractEmailShouldReturnEmailFromValidToken() {
        String token = jwtService.generateToken(testEmail);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    // @Test
    // void isTokenValidShouldReturnTrueForValidToken() {
    // String token = jwtService.generateToken(testEmail);

    // boolean isValid = jwtService.isTokenValid(token);

    // assertThat(isValid).isTrue();
    // }

    // @Test
    // void isTokenValidShouldReturnFalseForInvalidToken() {
    // String invalidToken = "invalid.jwt.token";

    // boolean isValid = jwtService.isTokenValid(invalidToken);

    // assertThat(isValid).isFalse();
    // }

    // @Test
    // void isTokenValidShouldReturnFalseForExpiredToken() {
    // // This test would need a way to create an expired token
    // // For now, we'll test with an invalid format
    // String expiredToken =
    // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

    // boolean isValid = jwtService.isTokenValid(expiredToken);

    // assertThat(isValid).isFalse();
    // }

    @Test
    void extractEmailShouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token";

        assertThatThrownBy(() -> jwtService.extractEmail(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateTokenShouldCreateDifferentTokensForDifferentEmails() {
        String token1 = jwtService.generateToken("user1@example.com");
        String token2 = jwtService.generateToken("user2@example.com");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void tokenShouldContainEmailInClaims() {
        String testEmail = "claim@example.com";
        String token = jwtService.generateToken(testEmail);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(testEmail);
    }
}
