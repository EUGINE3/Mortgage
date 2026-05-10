package com.bank.mortgage.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DecisionRequest {

    @NotNull
    private String decisionType;

    @Size(max = 1000)
    private String reason;

    @NotNull
    private String decidedBy;
}
