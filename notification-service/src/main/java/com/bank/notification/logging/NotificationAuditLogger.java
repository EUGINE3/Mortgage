package com.bank.notification.logging;

import com.bank.notification.domain.NotificationLog;
import com.bank.notification.domain.enums.NotificationStatus;
import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationAuditLogger {

    public void logReceived(ApplicationEvent event, KafkaEventMetadata metadata) {
        log.info(
                "notification.received applicationId={} eventType={} status={} applicantEmail={} "
                        + "topic={} partition={} offset={} correlationId={} traceId={}",
                event.getApplicationId(),
                event.getEventType(),
                event.getStatus(),
                maskEmail(event.getApplicantEmail()),
                metadata.topic(),
                metadata.partition(),
                metadata.offset(),
                event.getCorrelationId(),
                event.getTraceId());
    }

    public void logSkipped(String reason, ApplicationEvent event, KafkaEventMetadata metadata) {
        log.info(
                "notification.skipped reason={} applicationId={} eventType={} topic={} partition={} offset={} "
                        + "correlationId={} traceId={}",
                reason,
                event.getApplicationId(),
                event.getEventType(),
                metadata.topic(),
                metadata.partition(),
                metadata.offset(),
                event.getCorrelationId(),
                event.getTraceId());
    }

    public void logPrepared(ApplicationEvent event, String subject, String message, NotificationStatus status) {
        log.info(
                "notification.prepared applicationId={} eventType={} recipient={} subject='{}' status={} "
                        + "correlationId={} traceId={} body='{}'",
                event.getApplicationId(),
                event.getEventType(),
                maskEmail(event.getApplicantEmail()),
                subject,
                status,
                event.getCorrelationId(),
                event.getTraceId(),
                message);
    }

    public void logRecorded(NotificationLog notificationLog) {
        log.info(
                "notification.recorded id={} applicationId={} eventType={} recipient={} subject='{}' status={} "
                        + "correlationId={} traceId={} createdAt={}",
                notificationLog.getId(),
                notificationLog.getApplicationId(),
                notificationLog.getEventType(),
                maskEmail(notificationLog.getRecipientEmail()),
                notificationLog.getSubject(),
                notificationLog.getStatus(),
                notificationLog.getCorrelationId(),
                notificationLog.getTraceId(),
                notificationLog.getCreatedAt());
    }

    public void logDeliveryFailure(ApplicationEvent event, Exception ex) {
        log.error(
                "notification.failed applicationId={} eventType={} recipient={} correlationId={} traceId={} error={}",
                event.getApplicationId(),
                event.getEventType(),
                maskEmail(event.getApplicantEmail()),
                event.getCorrelationId(),
                event.getTraceId(),
                ex.getMessage(),
                ex);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2
                ? "**"
                : local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return maskedLocal + "@" + parts[1];
    }
}
