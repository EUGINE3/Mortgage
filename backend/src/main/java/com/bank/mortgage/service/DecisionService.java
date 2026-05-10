package com.bank.mortgage.service;

import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.DecisionResponse;

import java.util.UUID;

public interface DecisionService {

    DecisionResponse create(UUID applicationId, DecisionRequest request);

    DecisionResponse getById(UUID id);
}
