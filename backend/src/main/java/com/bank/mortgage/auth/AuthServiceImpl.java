package com.bank.mortgage.auth;

import com.bank.mortgage.auth.dto.JwtResponse;
import com.bank.mortgage.auth.dto.LoginRequest;
import com.bank.mortgage.auth.dto.RegisterRequest;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.repository.UserRepository;
import com.bank.mortgage.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public JwtResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nationalId(request.getNationalId())
                .role(request.getRole() != null ? request.getRole() : "APPLICANT")
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role(user.getRole())
                .build();
    }

    @Override
    public JwtResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role(user.getRole())
                .build();
    }
}
