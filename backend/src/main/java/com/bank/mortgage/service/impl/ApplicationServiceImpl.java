package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.events.EventPublisher;
import com.bank.mortgage.mapper.ApplicationMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;
    private final ApplicationMapper mapper;
    private final EventPublisher eventPublisher;

    @Override
    public ApplicationResponse create(ApplicationRequest request) {

        Application application = Application.builder()
                .id(UUID.randomUUID())
                .nationalId(request.getNationalId())
                .loanAmount(request.getLoanAmount())
                .tenureMonths(request.getTenureMonths())
                .income(request.getIncome())
                .status(ApplicationStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Application saved = repository.save(application);

        eventPublisher.publishApplicationCreated(saved);

        return mapper.toResponse(saved);
    }

    @Override
    public ApplicationResponse getById(UUID id) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        return mapper.toResponse(application);
    }
}
