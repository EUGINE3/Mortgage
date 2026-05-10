package com.bank.mortgage.service;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse create(ApplicationRequest request);

    ApplicationResponse getById(UUID id);

    PageResponse<ApplicationResponse> list(Pageable pageable);

    PageResponse<ApplicationResponse> filterByStatus(String status, Pageable pageable);

    PageResponse<ApplicationResponse> filterByApplicant(UUID applicantId, Pageable pageable);

    PageResponse<ApplicationResponse> filterByNationalId(String nationalId, Pageable pageable);

    PageResponse<ApplicationResponse> filterByDateRange(Instant startDate, Instant endDate, Pageable pageable);

    ApplicationResponse updateApplication(UUID id, ApplicationRequest request);

    ApplicationResponse approveOrReject(UUID id, DecisionRequest request);

    void deleteApplication(UUID id);
}
