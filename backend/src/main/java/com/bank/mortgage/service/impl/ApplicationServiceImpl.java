package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.events.EventPublisher;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.exception.UnauthorizedException;
import com.bank.mortgage.mapper.ApplicationMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.service.ApplicationService;
import com.bank.mortgage.util.SecurityUtil;
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
    private final SecurityUtil securityUtil;

    @Override
    public ApplicationResponse create(ApplicationRequest request) {
        User applicant = securityUtil.getCurrentUser();

        Application application = Application.builder()
                .id(UUID.randomUUID())
                .applicant(applicant)
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

        // Check authorization
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(application.getApplicant())) {
            throw new UnauthorizedException("You can only view your own applications");
        }

        return mapper.toResponse(application);
    }

    @Override
    public PageResponse<ApplicationResponse> list(Pageable pageable) {
        Page<Application> page;
        
        if (securityUtil.isApplicant()) {
            // Applicants see only their own applications
            User currentUser = securityUtil.getCurrentUser();
            page = repository.findByApplicant(currentUser, pageable);
        } else {
            // Credit officers see all applications
            page = repository.findAll(pageable);
        }
        
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByStatus(String status, Pageable pageable) {
        if (!securityUtil.isCreditOfficer()) {
            throw new UnauthorizedException("Only credit officers can filter by status");
        }
        
        ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
        Page<Application> page = repository.findByStatus(appStatus, pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByApplicant(UUID applicantId, Pageable pageable) {
        if (!securityUtil.isCreditOfficer()) {
            throw new UnauthorizedException("Only credit officers can filter by applicant");
        }
        
        // Placeholder - would need to fetch User by ID
        Page<Application> page = repository.findAll(pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByNationalId(String nationalId, Pageable pageable) {
        if (!securityUtil.isCreditOfficer()) {
            throw new UnauthorizedException("Only credit officers can filter by national ID");
        }
        
        Page<Application> page = repository.findByNationalId(nationalId, pageable);
        return buildPageResponse(page);
    }

    @Override
    public PageResponse<ApplicationResponse> filterByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        if (!securityUtil.isCreditOfficer()) {
            throw new UnauthorizedException("Only credit officers can filter by date range");
        }
        
        Page<Application> page = repository.findByCreatedAtBetween(startDate, endDate, pageable);
        return buildPageResponse(page);
    }

    @Override
    public ApplicationResponse updateApplication(UUID id, ApplicationRequest request) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found with ID: " + id));

        // Check authorization - only owner can edit their application
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(application.getApplicant())) {
            throw new UnauthorizedException("You can only edit your own applications");
        }

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
        if (!securityUtil.isCreditOfficer()) {
            throw new UnauthorizedException("Only credit officers can approve or reject applications");
        }

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

        // Check authorization - only owner (applicant) or credit officer can delete
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(application.getApplicant())) {
            throw new UnauthorizedException("You can only delete your own applications");
        }

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
