package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.events.EventPublisher;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.mapper.ApplicationMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new NotFoundException("Application not found with ID: " + id));

        return mapper.toResponse(application);
    }

    @Override
    public PageResponse<ApplicationResponse> list(Pageable pageable) {
        Page<Application> page = repository.findAll(pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByStatus(String status, Pageable pageable) {
        ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
        Page<Application> page = repository.findByStatus(appStatus, pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByApplicant(UUID applicantId, Pageable pageable) {
        Page<Application> page = repository.findByApplicantId(applicantId, pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByNationalId(String nationalId, Pageable pageable) {
        Page<Application> page = repository.findByNationalId(nationalId, pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        Page<Application> page = repository.findByCreatedAtBetween(startDate, endDate, pageable);
        return buildPageResponse(page);
    }

    @Override
    public ApplicationResponse updateApplication(UUID id, ApplicationRequest request) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found with ID: " + id));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Cannot update application that is not in PENDING status");
        }

        application.setLoanAmount(request.getLoanAmount());
        application.setTenureMonths(request.getTenureMonths());
        application.setIncome(request.getIncome());
        application.setUpdatedAt(Instant.now());

        Application updated = repository.save(application);
        return mapper.toResponse(updated);
    }

    @Override
    public ApplicationResponse approveOrReject(UUID id, DecisionRequest request) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found with ID: " + id));

        ApplicationStatus newStatus = ApplicationStatus.valueOf(request.getStatus().toUpperCase());
        application.setStatus(newStatus);
        application.setUpdatedAt(Instant.now());

        Application updated = repository.save(application);
        return mapper.toResponse(updated);
    }

    @Override
    public void deleteApplication(UUID id) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found with ID: " + id));

        repository.delete(application);
    }

    private PageResponse<ApplicationResponse> buildPageResponse(Page<Application> page) {
        return PageResponse.<ApplicationResponse>builder()
                .content(page.getContent().stream()
                        .map(mapper::toResponse)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
