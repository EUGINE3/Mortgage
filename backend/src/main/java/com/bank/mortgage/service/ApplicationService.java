package com.bank.mortgage.service;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;

import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse create(ApplicationRequest request);

    ApplicationResponse getById(UUID id);
}
