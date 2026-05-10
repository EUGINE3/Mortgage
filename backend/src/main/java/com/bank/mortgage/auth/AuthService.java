package com.bank.mortgage.auth;

import com.bank.mortgage.auth.dto.JwtResponse;
import com.bank.mortgage.auth.dto.LoginRequest;
import com.bank.mortgage.auth.dto.RegisterRequest;

public interface AuthService {

    JwtResponse register(RegisterRequest request);

    JwtResponse login(LoginRequest request);
}
