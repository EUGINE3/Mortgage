package com.bank.mortgage.util;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUtil securityUtil;

    private User applicant;
    private User creditOfficer;

    @BeforeEach
    void setUp() {
        applicant = User.builder()
                .id(UUID.randomUUID())
                .email("applicant@example.com")
                .role("APPLICANT")
                .build();

        creditOfficer = User.builder()
                .id(UUID.randomUUID())
                .email("officer@example.com")
                .role("CREDIT_OFFICER")
                .build();
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("applicant@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));

        User user = securityUtil.getCurrentUser();

        assertThat(user).isEqualTo(applicant);
    }

    @Test
    void getCurrentUserRoleShouldReturnApplicantRole() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("applicant@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));

        String role = securityUtil.getCurrentUserRole();

        assertThat(role).isEqualTo("APPLICANT");
    }

    @Test
    void isCurrentUserOwnerShouldReturnTrueForSameUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("applicant@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));

        boolean isOwner = securityUtil.isCurrentUserOwner(applicant);

        assertThat(isOwner).isTrue();
    }

    @Test
    void isCurrentUserOwnerShouldReturnFalseForDifferentUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("applicant@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));

        boolean isOwner = securityUtil.isCurrentUserOwner(creditOfficer);

        assertThat(isOwner).isFalse();
    }

    @Test
    void isCreditOfficerShouldReturnTrueForCreditOfficerRole() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("officer@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(creditOfficer));

        boolean isCreditOfficer = securityUtil.isCreditOfficer();

        assertThat(isCreditOfficer).isTrue();
    }

    @Test
    void isApplicantShouldReturnTrueForApplicantRole() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("applicant@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("applicant@example.com")).thenReturn(Optional.of(applicant));

        boolean isApplicant = securityUtil.isApplicant();

        assertThat(isApplicant).isTrue();
    }
}
