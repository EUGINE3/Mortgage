package com.bank.notification.consumer;

import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;
import com.bank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${notification.kafka.topic:loan.applications}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload ApplicationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = "correlation_id", required = false) String correlationId,
            @Header(value = "trace_id", required = false) String traceId) {

        if (event.getCorrelationId() == null && correlationId != null) {
            event.setCorrelationId(correlationId);
        }
        if (event.getTraceId() == null && traceId != null) {
            event.setTraceId(traceId);
        }

        log.info("Received application event type={} applicationId={} topic={} partition={} offset={} key={} correlationId={} traceId={}",
                event.getEventType(),
                event.getApplicationId(),
                topic,
                partition,
                offset,
                key,
                event.getCorrelationId(),
                event.getTraceId());

        notificationService.handleApplicationEvent(event, new KafkaEventMetadata(topic, partition, offset));
    }
}
