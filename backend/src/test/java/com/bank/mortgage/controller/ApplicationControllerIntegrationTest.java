package com.bank.mortgage.controller;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User applicant;
    private String applicantEmail;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        applicantEmail = "applicant@test.com";

        applicant = User.builder()
                .id(UUID.randomUUID())
                .email(applicantEmail)
                .password(passwordEncoder.encode("password123"))
                .fullName("Test Applicant")
                .role("APPLICANT")
                .createdAt(Instant.now())
                .build();

        userRepository.save(applicant);
    }

    @Test
    @WithMockUser(username = "applicant@test.com", roles = "APPLICANT")
    void createApplicationShouldReturnSuccessfulResponseWithValidRequest() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setNationalId("1234567890");
        request.setLoanAmount(BigDecimal.valueOf(100000));
        request.setTenureMonths(60);
        request.setIncome(BigDecimal.valueOf(50000));

        MvcResult result = mockMvc.perform(
                post("/api/v1/applications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ApplicationResponse response = objectMapper.readValue(content, ApplicationResponse.class);

        assertThat(response)
                .isNotNull()
                .extracting("loanAmount", "tenureMonths", "status")
                .contains(
                        BigDecimal.valueOf(100000),
                        60,
                        "PENDING"
                );
    }

    @Test
    void homeEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk());
    }
}
