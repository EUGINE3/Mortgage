package com.bank.mortgage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import com.bank.mortgage.domain.enums.UserRole;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private UUID id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;

    private String nationalId;

    
    //@Column(nullable = false)
    //private UserRole role; // APPLICANT or CREDIT_OFFICER
    
    private String role; // APPLICANT or CREDIT_OFFICER

    private Instant createdAt;
}
