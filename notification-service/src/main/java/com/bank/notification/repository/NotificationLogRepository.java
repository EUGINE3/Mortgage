package com.bank.notification.repository;

import com.bank.notification.domain.NotificationLog;
import com.bank.notification.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByApplicationIdAndEventTypeAndCorrelationId(
            UUID applicationId, String eventType, String correlationId);

    boolean existsByApplicationIdAndEventTypeAndCorrelationIdAndStatusIn(
            UUID applicationId,
            String eventType,
            String correlationId,
            java.util.Collection<NotificationStatus> statuses);

    Optional<NotificationLog> findByApplicationIdAndEventTypeAndCorrelationId(
            UUID applicationId, String eventType, String correlationId);
}
