package com.bank.mortgage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplicationRequest {

    @NotBlank
    private String nationalId;

    @NotNull
    private BigDecimal loanAmount;

    @NotNull
    private Integer tenureMonths;

    private BigDecimal income;
}
