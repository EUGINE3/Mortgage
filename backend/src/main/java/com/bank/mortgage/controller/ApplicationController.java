package com.bank.mortgage.controller;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.exception.MissingIdempotencyKeyException;
import com.bank.mortgage.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Applications", description = "Mortgage application management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Create a new mortgage application",
            description = "Requires X-Idempotency-Key header to safely retry duplicate submissions")
    @ApiResponse(responseCode = "201", description = "Application created successfully")
    @ApiResponse(responseCode = "400", description = "Missing idempotency key")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "409", description = "Duplicate request still processing")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Parameter(description = "Unique key for idempotent retries", required = true)
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ApplicationRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("X-Idempotency-Key");
        }
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APPLICANT', 'CREDIT_OFFICER')")
    @Operation(summary = "List applications (own for applicants, all for officers)")
    @ApiResponse(responseCode = "200", description = "Applications retrieved successfully")
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
