package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.events.EventPublisher;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.exception.UnauthorizedException;
import com.bank.mortgage.mapper.ApplicationMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

        @Mock
        private ApplicationRepository repository;

        @Mock
        private ApplicationMapper mapper;

        @Mock
        private EventPublisher eventPublisher;

        @Mock
        private SecurityUtil securityUtil;

        @InjectMocks
        private ApplicationServiceImpl service;

        private User applicant;

        @BeforeEach
        void setUp() {
                applicant = User.builder()
                                .id(UUID.randomUUID())
                                .email("applicant@example.com")
                                .role("APPLICANT")
                                .build();
        }

        @Test
        void createApplicationShouldSaveApplicationAndReturnResponse() {
                ApplicationRequest request = new ApplicationRequest();
                request.setNationalId("1234567890");
                request.setLoanAmount(BigDecimal.valueOf(10000));
                request.setTenureMonths(12);
                request.setIncome(BigDecimal.valueOf(5000));

                when(securityUtil.getCurrentUser()).thenReturn(applicant);

                Application saved = Application.builder()
                                .id(UUID.randomUUID())
                                .applicant(applicant)
                                .nationalId(request.getNationalId())
                                .status(ApplicationStatus.PENDING)
                                .loanAmount(request.getLoanAmount())
                                .tenureMonths(request.getTenureMonths())
                                .income(request.getIncome())
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                when(repository.save(any(Application.class))).thenReturn(saved);

                ApplicationResponse expectedResponse = ApplicationResponse.builder()
                                .id(saved.getId())
                                .status(saved.getStatus().name())
                                .loanAmount(saved.getLoanAmount())
                                .tenureMonths(saved.getTenureMonths())
                                .createdAt(saved.getCreatedAt())
                                .build();

                when(mapper.toResponse(saved)).thenReturn(expectedResponse);

                ApplicationResponse actualResponse = service.createApplication(request);

                assertThat(actualResponse).isSameAs(expectedResponse);

                ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
                verify(repository).save(captor.capture());
                verify(eventPublisher).publishApplicationCreated(saved);

                Application captured = captor.getValue();
                assertThat(captured.getApplicant()).isEqualTo(applicant);
                assertThat(captured.getNationalId()).isEqualTo(request.getNationalId());
                assertThat(captured.getLoanAmount()).isEqualTo(request.getLoanAmount());
                assertThat(captured.getTenureMonths()).isEqualTo(request.getTenureMonths());
                assertThat(captured.getIncome()).isEqualTo(request.getIncome());
                assertThat(captured.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        void getApplicationByIdShouldThrowNotFoundExceptionWhenMissing() {
                UUID id = UUID.randomUUID();
                when(repository.findById(id)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.getApplicationById(id))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Application not found with ID");
        }

        @Test
        void getApplicationByIdShouldThrowUnauthorizedExceptionForDifferentApplicant() {
                UUID id = UUID.randomUUID();
                User owner = User.builder().id(UUID.randomUUID()).email("owner@example.com").role("APPLICANT").build();
                Application application = Application.builder()
                                .id(id)
                                .applicant(owner)
                                .status(ApplicationStatus.PENDING)
                                .build();

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(owner)).thenReturn(false);

                assertThatThrownBy(() -> service.getApplicationById(id))
                                .isInstanceOf(UnauthorizedException.class)
                                .hasMessageContaining("not  authorizied to view this application");
        }

        @Test
        void getApplicationByIdShouldReturnApplicationForCreditOfficer() {
                UUID id = UUID.randomUUID();
                User owner = User.builder().id(UUID.randomUUID()).email("owner@example.com").role("APPLICANT").build();
                Application application = Application.builder()
                                .id(id)
                                .applicant(owner)
                                .status(ApplicationStatus.PENDING)
                                .loanAmount(BigDecimal.valueOf(50000))
                                .tenureMonths(24)
                                .createdAt(Instant.now())
                                .build();

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(false);

                ApplicationResponse expectedResponse = ApplicationResponse.builder()
                                .id(id)
                                .status(ApplicationStatus.PENDING.name())
                                .loanAmount(BigDecimal.valueOf(50000))
                                .tenureMonths(24)
                                .createdAt(application.getCreatedAt())
                                .build();

                when(mapper.toResponse(application)).thenReturn(expectedResponse);

                ApplicationResponse actualResponse = service.getApplicationById(id);

                assertThat(actualResponse).isEqualTo(expectedResponse);
        }

        @Test
        void deleteApplicationShouldThrowUnauthorizedExceptionForDifferentApplicant() {
                UUID id = UUID.randomUUID();
                User owner = User.builder().id(UUID.randomUUID()).email("owner@example.com").role("APPLICANT").build();
                Application application = Application.builder()
                                .id(id)
                                .applicant(owner)
                                .status(ApplicationStatus.PENDING)
                                .build();

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(owner)).thenReturn(false);

                assertThatThrownBy(() -> service.deleteApplication(id))
                                .isInstanceOf(UnauthorizedException.class)
                                .hasMessageContaining("only delete your own applications");
        }

        @Test
        void deleteApplicationShouldSucceedForOwner() {
                UUID id = UUID.randomUUID();
                Application application = Application.builder()
                                .id(id)
                                .applicant(applicant)
                                .status(ApplicationStatus.PENDING)
                                .build();

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

                service.deleteApplication(id);

                verify(repository).delete(application);
        }

        @Test
        void updateApplicationShouldThrowExceptionWhenNotPending() {
                UUID id = UUID.randomUUID();
                Application application = Application.builder()
                                .id(id)
                                .applicant(applicant)
                                .status(ApplicationStatus.APPROVED)
                                .build();

                ApplicationRequest request = new ApplicationRequest();
                request.setNationalId("1234567890");
                request.setLoanAmount(BigDecimal.valueOf(15000));
                request.setTenureMonths(24);

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

                assertThatThrownBy(() -> service.updateApplication(id, request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Cannot update application that is not in PENDING status");
        }

        @Test
        void updateApplicationShouldSucceedForOwnerWithPendingStatus() {
                UUID id = UUID.randomUUID();
                Application application = Application.builder()
                                .id(id)
                                .applicant(applicant)
                                .status(ApplicationStatus.PENDING)
                                .loanAmount(BigDecimal.valueOf(10000))
                                .tenureMonths(12)
                                .income(BigDecimal.valueOf(5000))
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                ApplicationRequest request = new ApplicationRequest();
                request.setNationalId("1234567890");
                request.setLoanAmount(BigDecimal.valueOf(15000));
                request.setTenureMonths(24);
                request.setIncome(BigDecimal.valueOf(6000));

                when(repository.findById(id)).thenReturn(Optional.of(application));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

                Application updated = Application.builder()
                                .id(id)
                                .applicant(applicant)
                                .status(ApplicationStatus.PENDING)
                                .loanAmount(request.getLoanAmount())
                                .tenureMonths(request.getTenureMonths())
                                .income(request.getIncome())
                                .createdAt(application.getCreatedAt())
                                .updatedAt(Instant.now())
                                .build();

                when(repository.save(any(Application.class))).thenReturn(updated);

                ApplicationResponse expectedResponse = ApplicationResponse.builder()
                                .id(id)
                                .status(ApplicationStatus.PENDING.name())
                                .loanAmount(request.getLoanAmount())
                                .tenureMonths(request.getTenureMonths())
                                .createdAt(application.getCreatedAt())
                                .build();

                when(mapper.toResponse(updated)).thenReturn(expectedResponse);

                ApplicationResponse actualResponse = service.updateApplication(id, request);

                assertThat(actualResponse).isEqualTo(expectedResponse);
                verify(repository).save(any(Application.class));
        }
}
