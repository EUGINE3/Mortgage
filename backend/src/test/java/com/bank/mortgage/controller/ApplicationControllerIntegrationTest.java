package com.bank.mortgage.controller;

import com.bank.mortgage.config.TestConfig;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Import(TestConfig.class)
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User applicant;
    private User creditOfficer;

    @BeforeEach
    void setUp() {

        applicationRepository.deleteAll();
        userRepository.deleteAll();

        applicant = User.builder()
                .id(UUID.randomUUID())
                .email("applicant@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test Applicant")
                .role("APPLICANT")
                .createdAt(Instant.now())
                .build();

        creditOfficer = User.builder()
                .id(UUID.randomUUID())
                .email("officer@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test Officer")
                .role("CREDIT_OFFICER")
                .createdAt(Instant.now())
                .build();

        userRepository.save(applicant);
        userRepository.save(creditOfficer);
    }

    // ================= HOME =================

    @Test
    void homeEndpointShouldBeAccessible() throws Exception {

        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk());
    }

    // ================= CREATE =================

    @Test
    void createApplicationShouldWork() throws Exception {

        mockMvc.perform(post("/api/v1/applications")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }



    @Test
    void createApplicationWithoutIdempotencyKeyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_IDEMPOTENCY_KEY"));
    }

    // ================= DECISION FLOW =================

    @Test
    void creditOfficerCanApproveApplication() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andReturn();

        ApplicationResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        DecisionRequest decision = new DecisionRequest();
        decision.setStatus("APPROVED");

        mockMvc.perform(patch("/api/v1/applications/{id}/decision", created.getId())
                        .with(csrf())
                        .with(user(creditOfficer.getEmail()).roles("CREDIT_OFFICER"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void applicantCannotMakeDecision() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andReturn();

        ApplicationResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        DecisionRequest decision = new DecisionRequest();
        decision.setStatus("APPROVED");

        mockMvc.perform(patch("/api/v1/applications/{id}/decision", created.getId())
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isForbidden());
    }

    // ================= PAGINATION =================

    @Test
    void shouldListApplicationsWithPagination() throws Exception {

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/applications")
                            .header("X-Idempotency-Key", UUID.randomUUID().toString())
                            .with(csrf())
                            .with(user(applicant.getEmail()).roles("APPLICANT"))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/applications")
                        .param("page", "0")
                        .param("size", "2")
                        .with(user(creditOfficer.getEmail()).roles("CREDIT_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    // ================= GET BY ID =================

    @Test
    void shouldReturn404WhenApplicationNotFound() throws Exception {

        mockMvc.perform(get("/api/v1/applications/{id}", UUID.randomUUID())
                        .with(user(creditOfficer.getEmail()).roles("CREDIT_OFFICER")))
                .andExpect(status().isNotFound());
    }

    // ================= DELETE =================

    @Test
    void shouldDeleteApplication() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(csrf())
                        .with(user(applicant.getEmail()).roles("APPLICANT"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andReturn();

        ApplicationResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApplicationResponse.class
        );

        mockMvc.perform(delete("/api/v1/applications/{id}", created.getId())
                        .with(csrf())
                        .with(user(creditOfficer.getEmail()).roles("CREDIT_OFFICER")))
                .andExpect(status().isNoContent());
    }

    // ================= HELPERS =================

    private ApplicationRequest validRequest() {
        ApplicationRequest request = new ApplicationRequest();
        request.setNationalId("1234567890");
        request.setLoanAmount(BigDecimal.valueOf(100000));
        request.setTenureMonths(60);
        request.setIncome(BigDecimal.valueOf(50000));
        return request;
    }
}
