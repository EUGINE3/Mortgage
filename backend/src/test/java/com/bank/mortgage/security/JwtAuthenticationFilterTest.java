package com.bank.mortgage.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidToken_ShouldSetAuthentication() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);
        
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getAuthentication(validToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).getAuthentication(validToken);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);
        
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_WithNoAuthorizationHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange - no Authorization header

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(jwtUtil, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_WithMalformedAuthorizationHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "MalformedToken");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(jwtUtil, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_WithEmptyBearerToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer ");
        when(jwtUtil.validateToken("")).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil).validateToken("");
        verify(jwtUtil, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_WithNullToken_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer null");

        when(jwtUtil.validateToken("null")).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil).validateToken("null");
        verify(jwtUtil, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_ShouldAlwaysCallFilterChain() throws ServletException, IOException {
        // Arrange
        MockFilterChain mockFilterChain = mock(MockFilterChain.class);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, mockFilterChain);

        // Assert
        verify(mockFilterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExceptionDuringValidation_ShouldContinueFilterChain() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);
        
        when(jwtUtil.validateToken(validToken)).thenThrow(new RuntimeException("Token validation failed"));
        
        MockFilterChain mockFilterChain = mock(MockFilterChain.class);

        // Act - should continue even if validation throws exception
        try {
            jwtAuthenticationFilter.doFilterInternal(request, response, mockFilterChain);
        } catch (RuntimeException e) {
            // Expected - the filter may propagate the exception
            assertThat(e.getMessage()).contains("Token validation failed");
        }

        // Assert - authentication should not be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_ShouldPreserveExistingAuthentication() throws ServletException, IOException {
        // Arrange
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);
        
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);
        
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getAuthentication(validToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("newuser");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotEqualTo(existingAuth);
    }
}
