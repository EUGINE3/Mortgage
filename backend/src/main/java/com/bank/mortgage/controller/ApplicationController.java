package com.bank.mortgage.controller;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('APPLICANT')")
    public ApplicationResponse create(
            @Valid @RequestBody ApplicationRequest request
    ) {
        return applicationService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'C    public ApplicationResponse getById(@PathVariable UUID id) {
REDIT_OFFICER')")
        return applicationService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT', 'CREDIT_OFFICER')")
    public PageResponse<ApplicationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return applicationService.list(pageable);
    }

    @GetMapping("/filter/status")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public PageResponse<ApplicationResponse> filterByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return applicationService.filterByStatus(status, pageable);
    }

    @GetMapping("/filter/applicant/{applicantId}")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public PageResponse<ApplicationResponse> filterByApplicant(
            @PathVariable UUID applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return applicationService.filterByApplicant(applicantId, pageable);
    }

    @GetMapping("/filter/national-id")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public PageResponse<ApplicationResponse> filterByNationalId(
            @RequestParam String nationalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return applicationService.filterByNationalId(nationalId, pageable);
    }

    @GetMapping("/filter/date-range")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public PageResponse<ApplicationResponse> filterByDateRange(
            @RequestParam Long startDate,
            @RequestParam Long endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return applicationService.filterByDateRange(
                Instant.ofEpochMilli(startDate),
                Instant.ofEpochMilli(endDate),
                pageable
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ApplicationResponse updateApplication(
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return applicationService.updateApplication(id, request);
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public ApplicationResponse approveOrReject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request
    ) {
        return applicationService.approveOrReject(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public void deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
    }
}
