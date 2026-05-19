package com.bank.mortgage.security;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private CustomUserDetailsService service;

        @Test
        void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
                String email = "user@example.com";
                User user = User.builder()
                                .id(UUID.randomUUID())
                                .email(email)
                                .password("encoded-password")
                                .fullName("John Doe")
                                .role("APPLICANT")
                                .build();

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

                UserDetails userDetails = service.loadUserByUsername(email);

                assertThat(userDetails.getUsername()).isEqualTo(email);
                assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
                assertThat(userDetails.getAuthorities())
                                .hasSize(1)
                                .extracting(auth -> auth.getAuthority())
                                .contains("ROLE_APPLICANT");
        }

        @Test
        void loadUserByUsernameShouldThrowExceptionWhenUserNotFound() {
                String email = "nonexistent@example.com";
                when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.loadUserByUsername(email))
                                .isInstanceOf(UsernameNotFoundException.class)
                                .hasMessageContaining("User not found");
        }

        @Test
        void loadUserByUsernameShouldReturnCreditOfficerRole() {
                String email = "officer@example.com";
                User user = User.builder()
                                .id(UUID.randomUUID())
                                .email(email)
                                .password("encoded-password")
                                .fullName("Jane Officer")
                                .role("CREDIT_OFFICER")
                                .build();

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

                UserDetails userDetails = service.loadUserByUsername(email);

                assertThat(userDetails.getAuthorities())
                                .hasSize(1)
                                .extracting(auth -> auth.getAuthority())
                                .contains("ROLE_CREDIT_OFFICER");
        }
}
