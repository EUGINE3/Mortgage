package com.bank.mortgage.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {

    private String accessToken;

    private String tokenType;

    private String role;
}
