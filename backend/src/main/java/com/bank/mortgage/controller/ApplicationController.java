package com.bank.mortgage.controller;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.service.ApplicationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT', 'CREDIT_OFFICER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> listApplications(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ApplicationResponse> response = applicationService.listApplications(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'CREDIT_OFFICER')")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable UUID id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter/status")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> filterByStatus(
            @RequestParam String status,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ApplicationResponse> response = applicationService.filterByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter/national-id")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> filterByNationalId(
            @RequestParam String nationalId,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ApplicationResponse> response = applicationService.filterByNationalId(nationalId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter/date-range")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> filterByDateRange(
            @RequestParam long startDate,
            @RequestParam long endDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ApplicationResponse> response = applicationService.filterByDateRange(
                Instant.ofEpochMilli(startDate),
                Instant.ofEpochMilli(endDate),
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable UUID id,
            @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('CREDIT_OFFICER')")
    public ResponseEntity<ApplicationResponse> makeDecision(
            @PathVariable UUID id,
            @RequestBody DecisionRequest request) {
        ApplicationResponse response = applicationService.approveOrRejectApplication(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'CREDIT_OFFICER')")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
