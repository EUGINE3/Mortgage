package com.bank.notification.service.impl;

import com.bank.notification.channel.NotificationChannel;
import com.bank.notification.domain.NotificationLog;
import com.bank.notification.domain.enums.NotificationStatus;
import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;
import com.bank.notification.exception.NotificationDeliveryException;
import com.bank.notification.logging.NotificationAuditLogger;
import com.bank.notification.repository.NotificationLogRepository;
import com.bank.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final KafkaEventMetadata METADATA =
            new KafkaEventMetadata("loan.applications", 0, 42L);

    @Mock
    private NotificationChannel notificationChannel;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationAuditLogger notificationAuditLogger;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void handleApplicationEvent_sendsCreateNotification() {
        UUID applicationId = UUID.randomUUID();
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("CREATE")
                .applicationId(applicationId)
                .applicantEmail("applicant@example.com")
                .applicantName("John Applicant")
                .loanAmount(new BigDecimal("100000"))
                .status("PENDING")
                .correlationId("corr-1")
                .traceId("trace-1")
                .timestamp(Instant.now())
                .build();

        when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                .thenReturn(false);
        when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                eq(applicationId), eq("CREATE"), eq("corr-1"), any())).thenReturn(false);

        notificationService.handleApplicationEvent(event, METADATA);

        verify(notificationChannel).send(
                "applicant@example.com",
                "Mortgage Application Received",
                "Dear John Applicant, your mortgage application (ID: " + applicationId
                        + ") for $100,000.00 has been received and is under review."
        );

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(processedEventRepository).save(any());
    }

    @Test
    void handleApplicationEvent_skipsAlreadyProcessedOffset() {
        UUID applicationId = UUID.randomUUID();
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("UPDATE")
                .applicationId(applicationId)
                .applicantEmail("applicant@example.com")
                .correlationId("corr-2")
                .build();

        when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                .thenReturn(true);

        notificationService.handleApplicationEvent(event, METADATA);

        verify(notificationChannel, never()).send(any(), any(), any());
        verify(notificationLogRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handleApplicationEvent_skipsDuplicateBusinessEvent() {
        UUID applicationId = UUID.randomUUID();
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("UPDATE")
                .applicationId(applicationId)
                .applicantEmail("applicant@example.com")
                .correlationId("corr-2")
                .build();

        when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                .thenReturn(false);
        when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                eq(applicationId), eq("UPDATE"), eq("corr-2"), any())).thenReturn(true);

        notificationService.handleApplicationEvent(event, METADATA);

        verify(notificationChannel, never()).send(any(), any(), any());
        verify(notificationLogRepository, never()).save(any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void handleApplicationEvent_throwsOnDeliveryFailureForRetry() {
        UUID applicationId = UUID.randomUUID();
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("DELETE")
                .applicationId(applicationId)
                .applicantEmail("applicant@example.com")
                .applicantName("Jane Applicant")
                .correlationId("corr-3")
                .build();

        when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                .thenReturn(false);
        when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                eq(applicationId), eq("DELETE"), eq("corr-3"), any())).thenReturn(false);
        doThrow(new RuntimeException("smtp down"))
                .when(notificationChannel)
                .send(any(), any(), any());

        assertThatThrownBy(() -> notificationService.handleApplicationEvent(event, METADATA))
                .isInstanceOf(NotificationDeliveryException.class);

        verify(notificationLogRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }
}
