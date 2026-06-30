package com.bank.notification.service.impl;

import com.bank.notification.channel.NotificationChannel;
import com.bank.notification.domain.NotificationLog;
import com.bank.notification.domain.ProcessedEvent;
import com.bank.notification.domain.enums.NotificationStatus;
import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;
import com.bank.notification.exception.NotificationDeliveryException;
import com.bank.notification.logging.NotificationAuditLogger;
import com.bank.notification.repository.NotificationLogRepository;
import com.bank.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
    }

    private ApplicationEvent.ApplicationEventBuilder baseEvent() {
        return ApplicationEvent.builder()
                .applicationId(applicationId)
                .applicantEmail("applicant@example.com")
                .applicantName("John Applicant")
                .loanAmount(new BigDecimal("100000"))
                .correlationId("corr-1")
                .traceId("trace-1")
                .timestamp(Instant.now());
    }

    private void stubNewEvent(String eventType, String correlationId) {
        when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                .thenReturn(false);
        when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                eq(applicationId), eq(eventType), eq(correlationId), any())).thenReturn(false);
    }

    @Nested
    @DisplayName("CREATE events")
    class CreateEvents {

        @Test
        void sendsCreateNotification() {
            ApplicationEvent event = baseEvent().eventType("CREATE").build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Received",
                    "Dear John Applicant, your mortgage application (ID: " + applicationId
                            + ") for $100,000.00 has been received and is under review."
            );

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            NotificationLog saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(saved.getCorrelationId()).isEqualTo("corr-1");
            assertThat(saved.getTraceId()).isEqualTo("trace-1");
            verify(processedEventRepository).save(any());
        }
    }

    @Nested
    @DisplayName("UPDATE events")
    class UpdateEvents {

        @Test
        void sendsApprovedNotification() {
            ApplicationEvent event = baseEvent()
                    .eventType("UPDATE")
                    .status("APPROVED")
                    .correlationId("corr-approved")
                    .build();
            stubNewEvent("UPDATE", "corr-approved");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Approved",
                    "Dear John Applicant, congratulations! Your mortgage application (ID: " + applicationId
                            + ") for $100,000.00 has been approved."
            );
        }

        @Test
        void sendsRejectedNotification() {
            ApplicationEvent event = baseEvent()
                    .eventType("UPDATE")
                    .status("REJECTED")
                    .correlationId("corr-rejected")
                    .build();
            stubNewEvent("UPDATE", "corr-rejected");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Rejected",
                    "Dear John Applicant, your mortgage application (ID: " + applicationId
                            + ") has been rejected. Please contact your credit officer for details."
            );
        }

        @Test
        void sendsGenericUpdateNotification() {
            ApplicationEvent event = baseEvent()
                    .eventType("UPDATE")
                    .status("UNDER_REVIEW")
                    .correlationId("corr-update")
                    .build();
            stubNewEvent("UPDATE", "corr-update");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Updated",
                    "Dear John Applicant, your mortgage application (ID: " + applicationId
                            + ") has been updated. Current status: UNDER_REVIEW."
            );
        }

        @Test
        void usesPendingWhenUpdateStatusIsNull() {
            ApplicationEvent event = baseEvent()
                    .eventType("UPDATE")
                    .status(null)
                    .correlationId("corr-null-status")
                    .build();
            stubNewEvent("UPDATE", "corr-null-status");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    eq("applicant@example.com"),
                    eq("Mortgage Application Updated"),
                    org.mockito.ArgumentMatchers.contains("Current status: PENDING.")
            );
        }
    }

    @Nested
    @DisplayName("DELETE events")
    class DeleteEvents {

        @Test
        void sendsDeleteNotification() {
            ApplicationEvent event = baseEvent()
                    .eventType("DELETE")
                    .correlationId("corr-delete")
                    .build();
            stubNewEvent("DELETE", "corr-delete");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Cancelled",
                    "Dear John Applicant, your mortgage application (ID: " + applicationId + ") has been cancelled."
            );
        }
    }

    @Nested
    @DisplayName("Unknown event types")
    class UnknownEventTypes {

        @Test
        void sendsDefaultNotificationForUnknownEventType() {
            ApplicationEvent event = baseEvent()
                    .eventType("CUSTOM")
                    .correlationId("corr-custom")
                    .build();
            stubNewEvent("CUSTOM", "corr-custom");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    "applicant@example.com",
                    "Mortgage Application Update",
                    "Dear John Applicant, there is an update on your mortgage application (ID: " + applicationId + ")."
            );
        }
    }

    @Nested
    @DisplayName("Invalid events")
    class InvalidEvents {

        @Test
        void skipsWhenApplicationIdIsNull() {
            ApplicationEvent event = baseEvent().applicationId(null).eventType("CREATE").build();

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger, never()).logReceived(any(), any());
            verify(notificationChannel, never()).send(any(), any(), any());
            verify(notificationLogRepository, never()).save(any());
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void skipsWhenEventTypeIsBlank() {
            ApplicationEvent event = baseEvent().eventType("  ").build();

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger, never()).logReceived(any(), any());
            verify(notificationChannel, never()).send(any(), any(), any());
            verify(notificationLogRepository, never()).save(any());
        }

        @Test
        void skipsWhenEventTypeIsNull() {
            ApplicationEvent event = baseEvent().eventType(null).build();

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger, never()).logReceived(any(), any());
            verify(notificationChannel, never()).send(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Idempotency and deduplication")
    class Idempotency {

        @Test
        void skipsAlreadyProcessedOffset() {
            ApplicationEvent event = baseEvent().eventType("UPDATE").correlationId("corr-2").build();

            when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                    .thenReturn(true);

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger).logReceived(event, METADATA);
            verify(notificationAuditLogger).logSkipped("already_processed_offset", event, METADATA);
            verify(notificationChannel, never()).send(any(), any(), any());
            verify(notificationLogRepository, never()).save(any());
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void skipsDuplicateBusinessEvent() {
            ApplicationEvent event = baseEvent().eventType("UPDATE").correlationId("corr-2").build();

            when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                    .thenReturn(false);
            when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                    eq(applicationId), eq("UPDATE"), eq("corr-2"), any())).thenReturn(true);

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger).logSkipped("business_duplicate", event, METADATA);
            verify(notificationChannel, never()).send(any(), any(), any());
            verify(notificationLogRepository, never()).save(any());
            verify(processedEventRepository).save(any());
        }

        @Test
        void generatesCorrelationIdWhenMissing() {
            ApplicationEvent event = baseEvent()
                    .eventType("CREATE")
                    .correlationId(null)
                    .build();

            when(processedEventRepository.existsByTopicAndPartitionAndOffset("loan.applications", 0, 42L))
                    .thenReturn(false);
            when(notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                    eq(applicationId), eq("CREATE"), any(), any())).thenReturn(false);

            notificationService.handleApplicationEvent(event, METADATA);

            ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getCorrelationId()).isNotBlank();

            ArgumentCaptor<ProcessedEvent> eventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
            verify(processedEventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getCorrelationId()).isEqualTo(logCaptor.getValue().getCorrelationId());
        }
    }

    @Nested
    @DisplayName("Delivery")
    class Delivery {

        @Test
        void skipsDeliveryWhenApplicantEmailIsMissing() {
            ApplicationEvent event = baseEvent()
                    .eventType("CREATE")
                    .applicantEmail(null)
                    .build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel, never()).send(any(), any(), any());

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
            verify(processedEventRepository).save(any());
        }

        @Test
        void skipsDeliveryWhenApplicantEmailIsBlank() {
            ApplicationEvent event = baseEvent()
                    .eventType("CREATE")
                    .applicantEmail("   ")
                    .build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel, never()).send(any(), any(), any());

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        }

        @Test
        void throwsOnDeliveryFailureForRetry() {
            ApplicationEvent event = baseEvent()
                    .eventType("DELETE")
                    .applicantName("Jane Applicant")
                    .correlationId("corr-3")
                    .build();
            stubNewEvent("DELETE", "corr-3");
            doThrow(new RuntimeException("smtp down"))
                    .when(notificationChannel)
                    .send(any(), any(), any());

            assertThatThrownBy(() -> notificationService.handleApplicationEvent(event, METADATA))
                    .isInstanceOf(NotificationDeliveryException.class)
                    .hasMessageContaining(applicationId.toString());

            verify(notificationAuditLogger).logDeliveryFailure(eq(event), any(RuntimeException.class));
            verify(notificationLogRepository, never()).save(any());
            verify(processedEventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Content defaults")
    class ContentDefaults {

        @Test
        void usesApplicantWhenNameIsMissing() {
            ApplicationEvent event = baseEvent()
                    .eventType("CREATE")
                    .applicantName(null)
                    .build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    eq("applicant@example.com"),
                    eq("Mortgage Application Received"),
                    org.mockito.ArgumentMatchers.startsWith("Dear Applicant,")
            );
        }

        @Test
        void usesPlaceholderWhenLoanAmountIsMissing() {
            ApplicationEvent event = baseEvent()
                    .eventType("CREATE")
                    .loanAmount(null)
                    .build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationChannel).send(
                    eq("applicant@example.com"),
                    eq("Mortgage Application Received"),
                    org.mockito.ArgumentMatchers.contains("for your requested amount has been received")
            );
        }
    }

    @Nested
    @DisplayName("Audit logging")
    class AuditLogging {

        @Test
        void logsFullLifecycleOnSuccessfulDelivery() {
            ApplicationEvent event = baseEvent().eventType("CREATE").build();
            stubNewEvent("CREATE", "corr-1");
            when(notificationLogRepository.save(any(NotificationLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.handleApplicationEvent(event, METADATA);

            verify(notificationAuditLogger).logReceived(event, METADATA);
            verify(notificationAuditLogger).logPrepared(
                    eq(event),
                    eq("Mortgage Application Received"),
                    any(),
                    eq(NotificationStatus.SENT));
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationAuditLogger).logRecorded(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        }

        @Test
        void recordsProcessedEventWithKafkaMetadata() {
            ApplicationEvent event = baseEvent().eventType("CREATE").build();
            stubNewEvent("CREATE", "corr-1");

            notificationService.handleApplicationEvent(event, METADATA);

            ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
            verify(processedEventRepository).save(captor.capture());
            ProcessedEvent recorded = captor.getValue();
            assertThat(recorded.getTopic()).isEqualTo("loan.applications");
            assertThat(recorded.getPartition()).isZero();
            assertThat(recorded.getOffset()).isEqualTo(42L);
            assertThat(recorded.getApplicationId()).isEqualTo(applicationId);
            assertThat(recorded.getEventType()).isEqualTo("CREATE");
            assertThat(recorded.getCorrelationId()).isEqualTo("corr-1");
            assertThat(recorded.getProcessedAt()).isNotNull();
        }
    }
}
