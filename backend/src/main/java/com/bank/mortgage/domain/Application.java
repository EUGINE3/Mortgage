package com.bank.mortgage.domain;

import com.bank.mortgage.domain.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    private UUID id;

    private UUID applicantId;

    private String nationalId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private BigDecimal loanAmount;

    private Integer tenureMonths;

    private BigDecimal income;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Integer version;
}
