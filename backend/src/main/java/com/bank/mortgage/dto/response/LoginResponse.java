package com.bank.mortgage.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;

    private String email;

    private String firstName;

    private String lastName;

    private String role;

    private Long expiresIn;
}
