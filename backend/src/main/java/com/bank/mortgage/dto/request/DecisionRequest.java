package com.bank.mortgage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DecisionRequest {

    @NotBlank(message = "Status is required (APPROVED or REJECTED)")
    private String status;

    @NotBlank(message = "Comments are required")
    @Size(max = 1000, message = "Comments must not exceed 1000 characters")
    private String comments;

    private String approverName;
}
