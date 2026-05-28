package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import com.bank.mortgage.dto.request.ApplicationRequest;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.dto.response.PageResponse;
import com.bank.mortgage.events.EventPublisher;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.exception.UnauthorizedException;
import com.bank.mortgage.mapper.ApplicationMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private User creditOfficer;
    private UUID applicationId;
    private ApplicationRequest validRequest;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();

        applicant = User.builder()
                .id(UUID.randomUUID())
                .email("applicant@example.com")
                .role("APPLICANT")
                .build();

        creditOfficer = User.builder()
                .id(UUID.randomUUID())
                .email("officer@example.com")
                .role("CREDIT_OFFICER")
                .build();

        validRequest = new ApplicationRequest();
        validRequest.setNationalId("1234567890");
        validRequest.setLoanAmount(BigDecimal.valueOf(200000));
        validRequest.setTenureMonths(360);
        validRequest.setIncome(BigDecimal.valueOf(75000));
    }

    @Nested
    @DisplayName("Create Application Tests")
    class CreateApplicationTests {

        @Test
        @DisplayName("Should create application successfully with valid data")
        void createApplication_shouldSaveAndReturnResponse() {
            // Given
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            
            Application savedApp = buildApplication(ApplicationStatus.PENDING, applicant);
            when(repository.save(any(Application.class))).thenReturn(savedApp);
            
            ApplicationResponse expectedResponse = buildResponse(savedApp);
            when(mapper.toResponse(savedApp)).thenReturn(expectedResponse);

            // When
            ApplicationResponse response = service.createApplication(validRequest);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            assertThat(response.getStatus()).isEqualTo("PENDING");

            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            
            Application captured = captor.getValue();
            assertThat(captured.getApplicant()).isEqualTo(applicant);
            assertThat(captured.getNationalId()).isEqualTo(validRequest.getNationalId());
            assertThat(captured.getLoanAmount()).isEqualTo(validRequest.getLoanAmount());
            assertThat(captured.getTenureMonths()).isEqualTo(validRequest.getTenureMonths());
            assertThat(captured.getIncome()).isEqualTo(validRequest.getIncome());
            assertThat(captured.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(captured.getId()).isNotNull();
            assertThat(captured.getCreatedAt()).isNotNull();
            assertThat(captured.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should always set status to PENDING regardless of request")
        void createApplication_shouldAlwaysForcePendingStatus() {
            // Given
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            
            ApplicationResponse mockResponse = ApplicationResponse.builder()
                    .id(applicationId)
                    .status("PENDING")
                    .build();
            when(mapper.toResponse(any(Application.class))).thenReturn(mockResponse);

            // When
            service.createApplication(validRequest);

            // Then
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("Should generate unique ID for each application")
        void createApplication_shouldGenerateUniqueId() {
            // Given
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenAnswer(i -> {
                Application app = i.getArgument(0);
                return buildResponse(app);
            });

            // When
            ApplicationResponse response1 = service.createApplication(validRequest);
            ApplicationResponse response2 = service.createApplication(validRequest);

            // Then
            assertThat(response1.getId()).isNotEqualTo(response2.getId());
        }

        @Test
        @DisplayName("Should handle null values in request gracefully")
        void createApplication_withNullValues_shouldHandleGracefully() {
            // Given
            ApplicationRequest incompleteRequest = new ApplicationRequest();
            
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(
                    ApplicationResponse.builder().id(applicationId).build()
            );

            // When
            ApplicationResponse response = service.createApplication(incompleteRequest);

            // Then
            assertThat(response).isNotNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            
            Application captured = captor.getValue();
            assertThat(captured.getLoanAmount()).isNull();
            assertThat(captured.getTenureMonths()).isNull();
            assertThat(captured.getIncome()).isNull();
        }
    }

    @Nested
    @DisplayName("Get Application Tests")
    class GetApplicationTests {

        @Test
        @DisplayName("Should return application when found and authorized")
        void getApplication_shouldReturnWhenFound() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
            
            ApplicationResponse expectedResponse = buildResponse(app);
            when(mapper.toResponse(app)).thenReturn(expectedResponse);

            // When
            ApplicationResponse response = service.getApplicationById(applicationId);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            verify(repository).findById(applicationId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when application doesn't exist")
        void getApplication_shouldThrowNotFound_whenMissing() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getApplicationById(applicationId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Application not found");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant not owner")
        void getApplication_shouldThrowUnauthorized_whenNotOwner() {
            // Given
            User otherUser = User.builder().id(UUID.randomUUID()).role("APPLICANT").build();
            Application app = buildApplication(ApplicationStatus.PENDING, otherUser);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(otherUser)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.getApplicationById(applicationId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You are not  authorizied to view this application");
        }

        @Test
        @DisplayName("Should allow credit officer to access any application")
        void getApplication_shouldAllowCreditOfficerAccess() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(false);
            
            ApplicationResponse expectedResponse = buildResponse(app);
            when(mapper.toResponse(app)).thenReturn(expectedResponse);

            // When
            ApplicationResponse response = service.getApplicationById(applicationId);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("List Applications Tests")
    class ListApplicationsTests {

        @Test
        @DisplayName("Should list only applicant's own applications when role is APPLICANT")
        void listApplications_asApplicant_shouldReturnOwnApplications() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Application> applications = IntStream.range(0, 3)
                    .mapToObj(i -> buildApplication(ApplicationStatus.PENDING, applicant))
                    .toList();
            Page<Application> page = new PageImpl<>(applications, pageable, 3);

            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.findByApplicant(applicant, pageable)).thenReturn(page);
            
            applications.forEach(app -> when(mapper.toResponse(app)).thenReturn(buildResponse(app)));

            // When
            PageResponse<ApplicationResponse> response = service.listApplications(pageable);

            // Then
            assertThat(response.getContent()).hasSize(3);
            assertThat(response.getTotalElements()).isEqualTo(3);
            assertThat(response.getPageNumber()).isEqualTo(0);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isLast()).isTrue();
            
            verify(repository).findByApplicant(applicant, pageable);
            verify(repository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should list all applications when role is CREDIT_OFFICER")
        void listApplications_asCreditOfficer_shouldReturnAllApplications() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User anotherApplicant = User.builder().id(UUID.randomUUID()).build();
            
            List<Application> applications = List.of(
                    buildApplication(ApplicationStatus.PENDING, applicant),
                    buildApplication(ApplicationStatus.APPROVED, anotherApplicant),
                    buildApplication(ApplicationStatus.REJECTED, applicant)
            );
            Page<Application> page = new PageImpl<>(applications, pageable, 3);

            when(securityUtil.isApplicant()).thenReturn(false);
            when(repository.findAll(pageable)).thenReturn(page);
            
            applications.forEach(app -> when(mapper.toResponse(app)).thenReturn(buildResponse(app)));

            // When
            PageResponse<ApplicationResponse> response = service.listApplications(pageable);

            // Then
            assertThat(response.getContent()).hasSize(3);
            assertThat(response.getTotalElements()).isEqualTo(3);
            
            verify(repository).findAll(pageable);
            verify(repository, never()).findByApplicant(any(), any());
        }

        @Test
        @DisplayName("Should handle empty results")
        void listApplications_whenEmpty_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> emptyPage = Page.empty(pageable);

            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.findByApplicant(applicant, pageable)).thenReturn(emptyPage);

            // When
            PageResponse<ApplicationResponse> response = service.listApplications(pageable);

            // Then
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
            assertThat(response.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle different page sizes and numbers")
        void listApplications_withVariousPageParams_shouldRespectPagination() {
            // Given
            Pageable pageable = PageRequest.of(2, 5);
            List<Application> applications = IntStream.range(0, 5)
                    .mapToObj(i -> buildApplication(ApplicationStatus.PENDING, applicant))
                    .toList();
            Page<Application> page = new PageImpl<>(applications, pageable, 25);

            when(securityUtil.isApplicant()).thenReturn(false);
            when(repository.findAll(pageable)).thenReturn(page);
            applications.forEach(app -> when(mapper.toResponse(app)).thenReturn(buildResponse(app)));

            // When
            PageResponse<ApplicationResponse> response = service.listApplications(pageable);

            // Then
            assertThat(response.getPageNumber()).isEqualTo(2);
            assertThat(response.getPageSize()).isEqualTo(5);
            assertThat(response.getTotalPages()).isEqualTo(5);
            assertThat(response.getTotalElements()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should support sorting when listing applications")
        void listApplications_withSorting_shouldRespectSortOrder() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            when(securityUtil.isApplicant()).thenReturn(false);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // When
            PageResponse<ApplicationResponse> response = service.listApplications(pageable);

            // Then
            assertThat(response).isNotNull();
            verify(repository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Filter By Status Tests")
    class FilterByStatusTests {

        @Test
        @DisplayName("Should filter applications by status for credit officer")
        void filterByStatus_asCreditOfficer_shouldReturnFilteredResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Application> applications = List.of(
                    buildApplication(ApplicationStatus.APPROVED, applicant)
            );
            Page<Application> page = new PageImpl<>(applications, pageable, 1);

            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByStatus(ApplicationStatus.APPROVED, pageable)).thenReturn(page);
            when(mapper.toResponse(any(Application.class))).thenReturn(
                    ApplicationResponse.builder().status("APPROVED").build()
            );

            // When
            PageResponse<ApplicationResponse> response = service.filterByStatus("APPROVED", pageable);

            // Then
            assertThat(response.getContent()).hasSize(1);
            verify(repository).findByStatus(ApplicationStatus.APPROVED, pageable);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant tries to filter by status")
        void filterByStatus_asApplicant_shouldThrowUnauthorized() {
            // Given
            when(securityUtil.isCreditOfficer()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.filterByStatus("PENDING", PageRequest.of(0, 10)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Only credit officers can filter by status");
            
            verify(repository, never()).findByStatus(any(), any());
        }

        @ParameterizedTest
        @EnumSource(ApplicationStatus.class)
        @DisplayName("Should handle all possible status values")
        void filterByStatus_withAllStatuses_shouldWork(ApplicationStatus status) {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> emptyPage = Page.empty(pageable);
            
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByStatus(status, pageable)).thenReturn(emptyPage);

            // When
            PageResponse<ApplicationResponse> response = service.filterByStatus(status.name(), pageable);

            // Then
            assertThat(response).isNotNull();
            verify(repository).findByStatus(status, pageable);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid status")
        void filterByStatus_withInvalidStatus_shouldThrowException() {
            // Given
            when(securityUtil.isCreditOfficer()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.filterByStatus("INVALID_STATUS", PageRequest.of(0, 10)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Filter By National ID Tests")
    class FilterByNationalIdTests {

        @Test
        @DisplayName("Should filter applications by national ID for credit officer")
        void filterByNationalId_asCreditOfficer_shouldReturnFilteredResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            String nationalId = "1234567890";
            List<Application> applications = List.of(
                    buildApplication(ApplicationStatus.PENDING, applicant)
            );
            Page<Application> page = new PageImpl<>(applications, pageable, 1);

            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByNationalId(nationalId, pageable)).thenReturn(page);
            when(mapper.toResponse(any(Application.class))).thenReturn(buildResponse(applications.get(0)));

            // When
            PageResponse<ApplicationResponse> response = service.filterByNationalId(nationalId, pageable);

            // Then
            assertThat(response.getContent()).hasSize(1);
            verify(repository).findByNationalId(nationalId, pageable);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant tries to filter by national ID")
        void filterByNationalId_asApplicant_shouldThrowUnauthorized() {
            // Given
            when(securityUtil.isCreditOfficer()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.filterByNationalId("1234567890", PageRequest.of(0, 10)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Only credit officers can filter by national ID");
        }

        @Test
        @DisplayName("Should handle non-existent national ID")
        void filterByNationalId_withNonExistentId_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            String nonExistentId = "9999999999";
            Page<Application> emptyPage = Page.empty(pageable);

            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByNationalId(nonExistentId, pageable)).thenReturn(emptyPage);

            // When
            PageResponse<ApplicationResponse> response = service.filterByNationalId(nonExistentId, pageable);

            // Then
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null national ID")
        void filterByNationalId_withNullId_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByNationalId(null, pageable)).thenReturn(Page.empty(pageable));

            // When
            PageResponse<ApplicationResponse> response = service.filterByNationalId(null, pageable);

            // Then
            assertThat(response.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filter By Date Range Tests")
    class FilterByDateRangeTests {

        @Test
        @DisplayName("Should filter applications by date range for credit officer")
        void filterByDateRange_asCreditOfficer_shouldReturnFilteredResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Instant startDate = Instant.now().minus(30, ChronoUnit.DAYS);
            Instant endDate = Instant.now();
            List<Application> applications = List.of(
                    buildApplication(ApplicationStatus.PENDING, applicant)
            );
            Page<Application> page = new PageImpl<>(applications, pageable, 1);

            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByCreatedAtBetween(startDate, endDate, pageable)).thenReturn(page);
            when(mapper.toResponse(any(Application.class))).thenReturn(buildResponse(applications.get(0)));

            // When
            PageResponse<ApplicationResponse> response = service.filterByDateRange(startDate, endDate, pageable);

            // Then
            assertThat(response.getContent()).hasSize(1);
            verify(repository).findByCreatedAtBetween(startDate, endDate, pageable);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant tries to filter by date range")
        void filterByDateRange_asApplicant_shouldThrowUnauthorized() {
            // Given
            when(securityUtil.isCreditOfficer()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.filterByDateRange(Instant.now().minus(30, ChronoUnit.DAYS), 
                    Instant.now(), PageRequest.of(0, 10)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Only credit officers can filter by date range");
        }

        @Test
        @DisplayName("Should handle date range with no results")
        void filterByDateRange_withNoResults_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Instant startDate = Instant.now().minus(30, ChronoUnit.DAYS);
            Instant endDate = Instant.now();
            Page<Application> emptyPage = Page.empty(pageable);

            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByCreatedAtBetween(startDate, endDate, pageable)).thenReturn(emptyPage);

            // When
            PageResponse<ApplicationResponse> response = service.filterByDateRange(startDate, endDate, pageable);

            // Then
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null date range")
        void filterByDateRange_withNullDates_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findByCreatedAtBetween(null, null, pageable)).thenReturn(Page.empty(pageable));

            // When
            PageResponse<ApplicationResponse> response = service.filterByDateRange(null, null, pageable);

            // Then
            assertThat(response.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Application Tests")
    class UpdateApplicationTests {

        @Test
        @DisplayName("Should update application successfully when pending and owner")
        void updateApplication_shouldSucceed_whenPendingAndOwner() {
            // Given
            Application existingApp = buildApplication(ApplicationStatus.PENDING, applicant);
            existingApp.setLoanAmount(BigDecimal.valueOf(100000));
            existingApp.setTenureMonths(240);
            existingApp.setIncome(BigDecimal.valueOf(50000));
            
            ApplicationRequest updateRequest = new ApplicationRequest();
            updateRequest.setLoanAmount(BigDecimal.valueOf(200000));
            updateRequest.setTenureMonths(360);
            updateRequest.setIncome(BigDecimal.valueOf(75000));

            when(repository.findById(applicationId)).thenReturn(Optional.of(existingApp));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            
            ApplicationResponse expectedResponse = buildResponse(existingApp);
            when(mapper.toResponse(any(Application.class))).thenReturn(expectedResponse);

            // When
            ApplicationResponse response = service.updateApplication(applicationId, updateRequest);

            // Then
            assertThat(response).isNotNull();
            
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            
            Application updated = captor.getValue();
            assertThat(updated.getLoanAmount()).isEqualTo(updateRequest.getLoanAmount());
            assertThat(updated.getTenureMonths()).isEqualTo(updateRequest.getTenureMonths());
            assertThat(updated.getIncome()).isEqualTo(updateRequest.getIncome());
            assertThat(updated.getUpdatedAt()).isNotNull();
            assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating non-existent application")
        void updateApplication_whenNotFound_shouldThrowException() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateApplication(applicationId, validRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Application not found");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant not owner")
        void updateApplication_whenNotOwner_shouldThrowUnauthorized() {
            // Given
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            Application app = buildApplication(ApplicationStatus.PENDING, otherUser);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(otherUser)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.updateApplication(applicationId, validRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only edit your own applications");
        }

        @Test
        @DisplayName("Should throw exception when updating non-pending application")
        void updateApplication_whenNotPending_shouldThrowException() {
            // Given
            Application approvedApp = buildApplication(ApplicationStatus.APPROVED, applicant);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(approvedApp));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.updateApplication(applicationId, validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot update application that is not in PENDING status");
            
            verify(repository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = ApplicationStatus.class, names = {"APPROVED", "REJECTED"})
        @DisplayName("Should throw exception for all non-pending statuses")
        void updateApplication_withNonPendingStatus_shouldThrowException(ApplicationStatus status) {
            // Given
            Application app = buildApplication(status, applicant);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.updateApplication(applicationId, validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot update application that is not in PENDING status");
        }

        @Test
        @DisplayName("Should allow credit officer to update any pending application")
        void updateApplication_creditOfficerUpdating_shouldSucceed() {
            // Given
            Application existingApp = buildApplication(ApplicationStatus.PENDING, applicant);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(existingApp));
            when(securityUtil.isApplicant()).thenReturn(false);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(buildResponse(existingApp));

            // When
            ApplicationResponse response = service.updateApplication(applicationId, validRequest);

            // Then
            assertThat(response).isNotNull();
            verify(repository).save(any(Application.class));
        }
    }

    @Nested
    @DisplayName("Approve or Reject Application Tests")
    class ApproveRejectApplicationTests {

        @Test
        @DisplayName("Should approve application when credit officer")
        void approveApplication_asCreditOfficer_shouldSucceed() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("APPROVED");
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            
            ApplicationResponse expectedResponse = buildResponse(app);
            when(mapper.toResponse(any(Application.class))).thenReturn(expectedResponse);

            // When
            ApplicationResponse response = service.approveOrRejectApplication(applicationId, decision);

            // Then
            assertThat(response).isNotNull();
            
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            
            Application updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(updated.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should reject application when credit officer")
        void rejectApplication_asCreditOfficer_shouldSucceed() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("REJECTED");
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(buildResponse(app));

            // When
            ApplicationResponse response = service.approveOrRejectApplication(applicationId, decision);

            // Then
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant tries to approve/reject")
        void approveOrReject_asApplicant_shouldThrowUnauthorized() {
            // Given
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("APPROVED");
            
            when(securityUtil.isCreditOfficer()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.approveOrRejectApplication(applicationId, decision))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Only credit officers can approve or reject applications");
        }

        @Test
        @DisplayName("Should throw NotFoundException when application doesn't exist")
        void approveOrReject_whenNotFound_shouldThrowException() {
            // Given
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("APPROVED");
            
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findById(applicationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.approveOrRejectApplication(applicationId, decision))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Application not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"APPROVED", "REJECTED"})
        @DisplayName("Should handle both approve and reject decisions")
        void approveOrReject_withBothDecisions_shouldWork(String decisionStatus) {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus(decisionStatus);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(buildResponse(app));

            // When
            ApplicationResponse response = service.approveOrRejectApplication(applicationId, decision);

            // Then
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus().name()).isEqualTo(decisionStatus);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid decision status")
        void approveOrReject_withInvalidStatus_shouldThrowException() {
            // Given
            DecisionRequest decision = new DecisionRequest();
            decision.setStatus("INVALID_STATUS");
            
            when(securityUtil.isCreditOfficer()).thenReturn(true);
            when(repository.findById(applicationId)).thenReturn(Optional.of(buildApplication(ApplicationStatus.PENDING, applicant)));

            // When & Then
            assertThatThrownBy(() -> service.approveOrRejectApplication(applicationId, decision))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Delete Application Tests")
    class DeleteApplicationTests {

        @Test
        @DisplayName("Should delete application when owner")
        void deleteApplication_whenOwner_shouldSucceed() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

            // When
            service.deleteApplication(applicationId);

            // Then
            verify(repository).delete(app);
        }

        @Test
        @DisplayName("Should allow credit officer to delete any application")
        void deleteApplication_creditOfficer_shouldSucceed() {
            // Given
            Application app = buildApplication(ApplicationStatus.PENDING, applicant);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(false);

            // When
            service.deleteApplication(applicationId);

            // Then
            verify(repository).delete(app);
        }

        @Test
        @DisplayName("Should throw NotFoundException when deleting non-existent application")
        void deleteApplication_whenNotFound_shouldThrowException() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.deleteApplication(applicationId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Application not found");
            
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when applicant not owner")
        void deleteApplication_whenNotOwner_shouldThrowUnauthorized() {
            // Given
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            Application app = buildApplication(ApplicationStatus.PENDING, otherUser);
            
            when(repository.findById(applicationId)).thenReturn(Optional.of(app));
            when(securityUtil.isApplicant()).thenReturn(true);
            when(securityUtil.isCurrentUserOwner(otherUser)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.deleteApplication(applicationId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You can only delete your own applications");
            
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete application regardless of status")
        void deleteApplication_withAnyStatus_shouldSucceed() {
            // Given
            ApplicationStatus[] statuses = {ApplicationStatus.PENDING, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED};
            
            for (ApplicationStatus status : statuses) {
                Application app = buildApplication(status, applicant);
                
                when(repository.findById(applicationId)).thenReturn(Optional.of(app));
                when(securityUtil.isApplicant()).thenReturn(true);
                when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
                
                if (status != statuses[0]) {
                    reset(repository);
                    when(repository.findById(applicationId)).thenReturn(Optional.of(app));
                }

                // When
                service.deleteApplication(applicationId);

                // Then
                verify(repository).delete(app);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very large loan amounts")
        void createApplication_withLargeLoanAmount_shouldHandle() {
            // Given
            validRequest.setLoanAmount(new BigDecimal("999999999999.99"));
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

            // When
            ApplicationResponse response = service.createApplication(validRequest);

            // Then
            assertThat(response).isNotNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getLoanAmount()).isEqualTo(validRequest.getLoanAmount());
        }

        @Test
        @DisplayName("Should handle maximum tenure months")
        void createApplication_withMaxTenure_shouldHandle() {
            // Given
            validRequest.setTenureMonths(600);
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

            // When
            ApplicationResponse response = service.createApplication(validRequest);

            // Then
            assertThat(response).isNotNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getTenureMonths()).isEqualTo(600);
        }

        @Test
        @DisplayName("Should handle zero loan amount")
        void createApplication_withZeroLoanAmount_shouldHandle() {
            // Given
            validRequest.setLoanAmount(BigDecimal.ZERO);
            when(securityUtil.getCurrentUser()).thenReturn(applicant);
            when(repository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

            // When
            ApplicationResponse response = service.createApplication(validRequest);

            // Then
            assertThat(response).isNotNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getLoanAmount()).isEqualTo(BigDecimal.ZERO);
        }
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private Application buildApplication(ApplicationStatus status, User user) {
        return Application.builder()
                .id(applicationId)
                .applicant(user)
                .nationalId(validRequest.getNationalId())
                .loanAmount(validRequest.getLoanAmount())
                .tenureMonths(validRequest.getTenureMonths())
                .income(validRequest.getIncome())
                .status(status)
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
