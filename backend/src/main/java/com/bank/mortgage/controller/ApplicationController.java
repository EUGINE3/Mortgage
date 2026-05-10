package com.bank.mortgage.controller;

import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(
            @Valid @RequestBody ApplicationRequest request
    ) {
        return applicationService.create(request);
    }

    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable UUID id) {
        return applicationService.getById(id);
    }
}
