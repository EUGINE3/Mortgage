package com.bank.mortgage.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {

    private UUID id;

    private String status;

    private BigDecimal loanAmount;

    private Integer tenureMonths;

    private Instant createdAt;
}
