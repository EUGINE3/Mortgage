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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository repository;
    @Mock private ApplicationMapper mapper;
    @Mock private EventPublisher eventPublisher;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private ApplicationServiceImpl service;

    private User applicant;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();

        applicant = User.builder()
                .id(UUID.randomUUID())
                .email("applicant@example.com")
                .role("APPLICANT")
                .build();
    }

    // =====================================================
    // CREATE APPLICATION
    // =====================================================

    @Test
    void createApplication_shouldSaveAndReturnResponse() {
        // Given
        ApplicationRequest request = buildRequest();

        when(securityUtil.getCurrentUser()).thenReturn(applicant);

        Application saved = buildApplication(ApplicationStatus.PENDING, applicant, request);

        when(repository.save(any(Application.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(buildResponse(saved));

        // When
        ApplicationResponse response = service.createApplication(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("PENDING");

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(captor.capture());

        Application captured = captor.getValue();
        assertThat(captured.getApplicant()).isEqualTo(applicant);
        assertThat(captured.getStatus()).isEqualTo(ApplicationStatus.PENDING);

        verify(eventPublisher).publishApplicationCreated(saved);
    }

    @Test
    void createApplication_shouldAlwaysForcePendingStatus() {
        ApplicationRequest request = buildRequest();

        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(
                ApplicationResponse.builder().id(applicationId).status("PENDING").build()
        );

        service.createApplication(request);

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    // =====================================================
    // GET APPLICATION
    // =====================================================

    @Test
    void getApplication_shouldThrowNotFound_whenMissing() {
        when(repository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicationById(applicationId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getApplication_shouldThrowUnauthorized_whenNotOwner() {
        User owner = User.builder().id(UUID.randomUUID()).role("APPLICANT").build();

        Application app = Application.builder()
                .id(applicationId)
                .applicant(owner)
                .build();

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(owner)).thenReturn(false);

        assertThatThrownBy(() -> service.getApplicationById(applicationId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getApplication_shouldReturnForCreditOfficer() {
        User owner = User.builder().id(UUID.randomUUID()).role("APPLICANT").build();

        Application app = buildApplication(ApplicationStatus.PENDING, owner, buildRequest());

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(false);

        ApplicationResponse response = buildResponse(app);

        when(mapper.toResponse(app)).thenReturn(response);

        ApplicationResponse result = service.getApplicationById(applicationId);

        assertThat(result).isEqualTo(response);
    }

    // =====================================================
    // DELETE APPLICATION
    // =====================================================

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        when(repository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteApplication(applicationId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_shouldThrowUnauthorized_whenNotOwner() {
        User owner = User.builder().id(UUID.randomUUID()).role("APPLICANT").build();

        Application app = Application.builder()
                .id(applicationId)
                .applicant(owner)
                .status(ApplicationStatus.PENDING)
                .build();

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(owner)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteApplication(applicationId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void delete_shouldSucceed_whenOwner() {
        Application app = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .status(ApplicationStatus.PENDING)
                .build();

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

        service.deleteApplication(applicationId);

        verify(repository).delete(app);
    }

    // =====================================================
    // UPDATE APPLICATION
    // =====================================================

    @Test
    void update_shouldThrow_whenNotPending() {
        Application app = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .status(ApplicationStatus.APPROVED)
                .build();

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

        assertThatThrownBy(() -> service.updateApplication(applicationId, buildRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void update_shouldSucceed_whenPendingAndOwner() {
        Application app = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .status(ApplicationStatus.PENDING)
                .loanAmount(BigDecimal.valueOf(10000))
                .tenureMonths(12)
                .income(BigDecimal.valueOf(5000))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ApplicationRequest request = buildRequest();

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Application updated = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .status(ApplicationStatus.PENDING)
                .loanAmount(request.getLoanAmount())
                .tenureMonths(request.getTenureMonths())
                .income(request.getIncome())
                .createdAt(app.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(mapper.toResponse(any())).thenReturn(buildResponse(updated));

        ApplicationResponse response = service.updateApplication(applicationId, request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(repository).save(any(Application.class));
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private ApplicationRequest buildRequest() {
        ApplicationRequest request = new ApplicationRequest();
        request.setNationalId("1234567890");
        request.setLoanAmount(BigDecimal.valueOf(10000));
        request.setTenureMonths(12);
        request.setIncome(BigDecimal.valueOf(5000));
        return request;
    }

    private Application buildApplication(ApplicationStatus status, User user, ApplicationRequest request) {
        return Application.builder()
                .id(applicationId)
                .applicant(user)
                .status(status)
                .loanAmount(request.getLoanAmount())
                .tenureMonths(request.getTenureMonths())
                .income(request.getIncome())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ApplicationResponse buildResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .status(app.getStatus().name())
                .loanAmount(app.getLoanAmount())
                .tenureMonths(app.getTenureMonths())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
