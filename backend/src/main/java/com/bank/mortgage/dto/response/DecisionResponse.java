package com.bank.mortgage.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DecisionResponse {

    private UUID id;

    private UUID applicationId;

    private String decisionType;

    private String reason;

    private String decidedBy;

    private Instant decidedAt;

    private Instant createdAt;
}
