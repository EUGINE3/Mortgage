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
import com.bank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final EnumSet<NotificationStatus> SUCCESSFUL_STATUSES =
            EnumSet.of(NotificationStatus.SENT, NotificationStatus.SKIPPED);

    private final NotificationChannel notificationChannel;
    private final NotificationLogRepository notificationLogRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationAuditLogger notificationAuditLogger;

    @Override
    @Transactional
    public void handleApplicationEvent(ApplicationEvent event, KafkaEventMetadata metadata) {
        if (event.getApplicationId() == null || !StringUtils.hasText(event.getEventType())) {
            log.warn("Skipping invalid application event: {}", event);
            return;
        }

        notificationAuditLogger.logReceived(event, metadata);

        if (processedEventRepository.existsByTopicAndPartitionAndOffset(
                metadata.topic(), metadata.partition(), metadata.offset())) {
            notificationAuditLogger.logSkipped("already_processed_offset", event, metadata);
            return;
        }

        String correlationId = event.getCorrelationId() != null
                ? event.getCorrelationId()
                : UUID.randomUUID().toString();

        if (notificationLogRepository.existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
                event.getApplicationId(), event.getEventType(), correlationId, SUCCESSFUL_STATUSES)) {
            notificationAuditLogger.logSkipped("business_duplicate", event, metadata);
            recordProcessedEvent(event, metadata, correlationId);
            return;
        }

        NotificationContent content = buildNotificationContent(event);
        NotificationStatus status = deliverNotification(event, content);
        notificationAuditLogger.logPrepared(event, content.subject(), content.message(), status);

        NotificationLog notificationLog = notificationLogRepository.save(NotificationLog.builder()
                .id(UUID.randomUUID())
                .applicationId(event.getApplicationId())
                .eventType(event.getEventType())
                .recipientEmail(event.getApplicantEmail())
                .subject(content.subject())
                .message(content.message())
                .status(status)
                .correlationId(correlationId)
                .traceId(event.getTraceId())
                .createdAt(Instant.now())
                .build());

        notificationAuditLogger.logRecorded(notificationLog);
        recordProcessedEvent(event, metadata, correlationId);
    }

    private NotificationStatus deliverNotification(ApplicationEvent event, NotificationContent content) {
        if (!StringUtils.hasText(event.getApplicantEmail())) {
            return NotificationStatus.SKIPPED;
        }

        try {
            notificationChannel.send(event.getApplicantEmail(), content.subject(), content.message());
            return NotificationStatus.SENT;
        } catch (Exception ex) {
            notificationAuditLogger.logDeliveryFailure(event, ex);
            throw new NotificationDeliveryException(
                    "Failed to deliver notification for application " + event.getApplicationId(), ex);
        }
    }

    private void recordProcessedEvent(
            ApplicationEvent event, KafkaEventMetadata metadata, String correlationId) {
        processedEventRepository.save(ProcessedEvent.builder()
                .id(UUID.randomUUID())
                .topic(metadata.topic())
                .partition(metadata.partition())
                .offset(metadata.offset())
                .applicationId(event.getApplicationId())
                .eventType(event.getEventType())
                .correlationId(correlationId)
                .processedAt(Instant.now())
                .build());
    }

    private NotificationContent buildNotificationContent(ApplicationEvent event) {
        String applicantName = StringUtils.hasText(event.getApplicantName())
                ? event.getApplicantName()
                : "Applicant";

        return switch (event.getEventType()) {
            case "CREATE" -> new NotificationContent(
                    "Mortgage Application Received",
                    String.format(
                            "Dear %s, your mortgage application (ID: %s) for %s has been received and is under review.",
                            applicantName,
                            event.getApplicationId(),
                            formatLoanAmount(event)
                    )
            );
            case "UPDATE" -> buildUpdateNotification(event, applicantName);
            case "DELETE" -> new NotificationContent(
                    "Mortgage Application Cancelled",
                    String.format(
                            "Dear %s, your mortgage application (ID: %s) has been cancelled.",
                            applicantName,
                            event.getApplicationId()
                    )
            );
            default -> new NotificationContent(
                    "Mortgage Application Update",
                    String.format(
                            "Dear %s, there is an update on your mortgage application (ID: %s).",
                            applicantName,
                            event.getApplicationId()
                    )
            );
        };
    }

    private NotificationContent buildUpdateNotification(ApplicationEvent event, String applicantName) {
        if ("APPROVED".equalsIgnoreCase(event.getStatus())) {
            return new NotificationContent(
                    "Mortgage Application Approved",
                    String.format(
                            "Dear %s, congratulations! Your mortgage application (ID: %s) for %s has been approved.",
                            applicantName,
                            event.getApplicationId(),
                            formatLoanAmount(event)
                    )
            );
        }
        if ("REJECTED".equalsIgnoreCase(event.getStatus())) {
            return new NotificationContent(
                    "Mortgage Application Rejected",
                    String.format(
                            "Dear %s, your mortgage application (ID: %s) has been rejected. Please contact your credit officer for details.",
                            applicantName,
                            event.getApplicationId()
                    )
            );
        }
        return new NotificationContent(
                "Mortgage Application Updated",
                String.format(
                        "Dear %s, your mortgage application (ID: %s) has been updated. Current status: %s.",
                        applicantName,
                        event.getApplicationId(),
                        event.getStatus() != null ? event.getStatus() : "PENDING"
                )
        );
    }

    private String formatLoanAmount(ApplicationEvent event) {
        return event.getLoanAmount() != null
                ? String.format("$%,.2f", event.getLoanAmount())
                : "your requested amount";
    }

    private record NotificationContent(String subject, String message) {}
}
