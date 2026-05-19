package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Decision;
import com.bank.mortgage.domain.enums.DecisionType;
import com.bank.mortgage.dto.request.DecisionRequest;
import com.bank.mortgage.dto.response.DecisionResponse;
import com.bank.mortgage.repository.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionServiceImplTest {

    @Mock
    private DecisionRepository repository;

    @InjectMocks
    private DecisionServiceImpl service;

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
    }

    @Test
    void createShouldSaveDecisionAndReturnResponse() {
        DecisionRequest request = new DecisionRequest();
        request.setStatus("APPROVED");
        request.setComments("Application meets all criteria");
        request.setApproverName("Officer Smith");

        Decision saved = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .decisionType(DecisionType.APPROVED)
                .reason(request.getComments())
                .decidedBy(request.getApproverName())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(Decision.class))).thenReturn(saved);

        DecisionResponse response = service.create(applicationId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getApplicationId()).isEqualTo(applicationId);
        assertThat(response.getDecisionType()).isEqualTo("APPROVED");
        assertThat(response.getReason()).isEqualTo(request.getComments());
        assertThat(response.getDecidedBy()).isEqualTo(request.getApproverName());

        ArgumentCaptor<Decision> captor = ArgumentCaptor.forClass(Decision.class);
        verify(repository).save(captor.capture());

        Decision captured = captor.getValue();
        assertThat(captured.getApplicationId()).isEqualTo(applicationId);
        assertThat(captured.getDecisionType()).isEqualTo(DecisionType.APPROVED);
        assertThat(captured.getReason()).isEqualTo(request.getComments());
    }

    @Test
    void createShouldHandleRejectionDecision() {
        DecisionRequest request = new DecisionRequest();
        request.setStatus("REJECTED");
        request.setComments("Insufficient income");
        request.setApproverName("Officer Johnson");

        Decision saved = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .decisionType(DecisionType.REJECTED)
                .reason(request.getComments())
                .decidedBy(request.getApproverName())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(Decision.class))).thenReturn(saved);

        DecisionResponse response = service.create(applicationId, request);

        assertThat(response.getDecisionType()).isEqualTo("REJECTED");
        assertThat(response.getReason()).isEqualTo("Insufficient income");
    }

    @Test
    void getByIdShouldReturnDecision() {
        UUID decisionId = UUID.randomUUID();
        Instant now = Instant.now();

        Decision decision = Decision.builder()
                .id(decisionId)
                .applicationId(applicationId)
                .decisionType(DecisionType.APPROVED)
                .reason("Approved for processing")
                .decidedBy("Officer Smith")
                .decidedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(repository.findById(decisionId)).thenReturn(Optional.of(decision));

        DecisionResponse response = service.getById(decisionId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(decisionId);
        assertThat(response.getApplicationId()).isEqualTo(applicationId);
        assertThat(response.getDecisionType()).isEqualTo("APPROVED");
        assertThat(response.getReason()).isEqualTo("Approved for processing");

        verify(repository).findById(decisionId);
    }

    @Test
    void getByIdShouldThrowExceptionWhenNotFound() {
        UUID decisionId = UUID.randomUUID();
        when(repository.findById(decisionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(decisionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decision not found");
    }

    @Test
    void createShouldConvertStatusToUpperCase() {
        DecisionRequest request = new DecisionRequest();
        request.setStatus("approved");
        request.setComments("Valid application");
        request.setApproverName("Officer Test");

        Decision saved = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .decisionType(DecisionType.APPROVED)
                .reason(request.getComments())
                .decidedBy(request.getApproverName())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(Decision.class))).thenReturn(saved);

        DecisionResponse response = service.create(applicationId, request);

        assertThat(response.getDecisionType()).isEqualTo("APPROVED");

        ArgumentCaptor<Decision> captor = ArgumentCaptor.forClass(Decision.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDecisionType()).isEqualTo(DecisionType.APPROVED);
    }

    @Test
    void createShouldGenerateUniqueDecisionId() {
        DecisionRequest request = new DecisionRequest();
        request.setStatus("APPROVED");
        request.setComments("Test");
        request.setApproverName("Officer");

        Decision saved1 = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId)
                .decisionType(DecisionType.APPROVED)
                .reason(request.getComments())
                .decidedBy(request.getApproverName())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UUID applicationId2 = UUID.randomUUID();
        Decision saved2 = Decision.builder()
                .id(UUID.randomUUID())
                .applicationId(applicationId2)
                .decisionType(DecisionType.APPROVED)
                .reason(request.getComments())
                .decidedBy(request.getApproverName())
                .decidedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(Decision.class)))
                .thenReturn(saved1)
                .thenReturn(saved2);

        DecisionResponse response1 = service.create(applicationId, request);
        DecisionResponse response2 = service.create(applicationId2, request);

        assertThat(response1.getId()).isNotEqualTo(response2.getId());
    }
}
