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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    // ---------------- HOME ----------------

    @Nested
    class HomeEndpointTests {

        @Test
        void homeEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {

            mockMvc.perform(get("/api/v1/home"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Running"))
                    .andExpect(jsonPath("$.message")
                            .value("Welcome to Mortgage Service API"));
        }

        @Test
        void nonExistentEndpointShouldReturnNotFound() throws Exception {

            mockMvc.perform(get("/api/v1/unknown")
                            .with(user(applicant.getEmail())
                                    .authorities(new SimpleGrantedAuthority("APPLICANT"))))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------- CREATE ----------------

    @Nested
    class CreateApplicationTests {

        @Test
        @DisplayName("Should create application successfully")
        
        void createApplicationShouldWork() throws Exception {

            ApplicationRequest request = validRequest();

            MvcResult result = mockMvc.perform(post("/api/v1/applications")
                            .with(csrf())
                            .with(user(applicant.getEmail()).roles("APPLICANT"))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            ApplicationResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    ApplicationResponse.class
            );

            assertThat(response.getId()).isNotNull();
            assertThat(response.getLoanAmount())
                    .isEqualTo(BigDecimal.valueOf(100000));
        }
        

        @Test
        void createWithoutAuthShouldFail() throws Exception {

            mockMvc.perform(post("/api/v1/applications")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------- GET ----------------

    // @Nested
    // class GetApplicationTests {

    //     @Test
    //     void shouldReturn404WhenNotFound() throws Exception {

    //         mockMvc.perform(get("/api/v1/applications/{id}", UUID.randomUUID())
    //                         .with(user(applicant.getEmail())
    //                                 .authorities(new SimpleGrantedAuthority("APPLICANT"))))
    //                 .andExpect(status().isNotFound());
    //     }
    // }

    // ---------------- APPROVE / REJECT ----------------

    @Nested
    class ApproveRejectTests {

        @Test
        void applicantShouldNotApprove() throws Exception {

            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("APPROVED");

            mockMvc.perform(
                            patch("/api/v1/applications/{id}/decision", UUID.randomUUID())
                                    .contentType(APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(decision))
                                    .with(user(applicant.getEmail())
                                            .authorities(new SimpleGrantedAuthority("APPLICANT")))
                    )
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------- HELPERS ----------------

    private ApplicationRequest validRequest() {

        ApplicationRequest request = new ApplicationRequest();

        request.setNationalId("1234567890");
        request.setLoanAmount(BigDecimal.valueOf(100000));
        request.setTenureMonths(60);
        request.setIncome(BigDecimal.valueOf(50000));

        return request;
    }
}
