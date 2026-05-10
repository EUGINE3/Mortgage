package com.bank.mortgage.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {

    private Instant timestamp;

    private String errorCode;

    private String detail;
}
