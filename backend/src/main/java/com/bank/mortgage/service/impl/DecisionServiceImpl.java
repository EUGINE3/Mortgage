package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Decision;
import com.bank.mortgage.domain.enums.DecisionType;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.DecisionResponse;
import com.bank.mortgage.repository.DecisionRepository;
import com.bank.mortgage.service.DecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionRepository repository;

    @Override
    public DecisionResponse create(UUID applicationId, DecisionRequest request) {

        Decision decision = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .decisionType(DecisionType.valueOf(request.getDecisionType()))
                .reason(request.getReason())
                .decidedBy(request.getDecidedBy())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Decision saved = repository.save(decision);

        return toResponse(saved);
    }

    @Override
    public DecisionResponse getById(UUID id) {
        Decision decision = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Decision not found"));

        return toResponse(decision);
    }

    private DecisionResponse toResponse(Decision decision) {
        return DecisionResponse.builder()
                .id(decision.getId())
                .applicationId(decision.getApplicationId())
                .decisionType(decision.getDecisionType().name())
                .reason(decision.getReason())
                .decidedBy(decision.getDecidedBy())
                .decidedAt(decision.getDecidedAt())
                .createdAt(decision.getCreatedAt())
                .build();
    }
}
