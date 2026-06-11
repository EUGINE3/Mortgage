package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private String getTraceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void publishApplicationCreated(Application application) {
        ApplicationEvent event = ApplicationEvent.fromApplication(
                application,
                "CREATE",
                getCorrelationId(),
                getTraceId()
        );

        Message<ApplicationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka_messageKey", application.getId().toString())
                .setHeader("correlation_id", event.getCorrelationId())
                .setHeader("trace_id", event.getTraceId())
                .build();

        kafkaTemplate.send("loan.applications", message);
        log.info("Published application created event {} with correlationId {}", 
                application.getId(), event.getCorrelationId());
    }

    @Override
    public void publishApplicationUpdated(Application application) {
        ApplicationEvent event = ApplicationEvent.fromApplication(
                application,
                "UPDATE",
                getCorrelationId(),
                getTraceId()
        );

        Message<ApplicationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka_messageKey", application.getId().toString())
                .setHeader("correlation_id", event.getCorrelationId())
                .setHeader("trace_id", event.getTraceId())
                .build();

        kafkaTemplate.send("loan.applications", message);
        log.info("Published application updated event {} with correlationId {}", 
                application.getId(), event.getCorrelationId());
    }

    @Override
    public void publishApplicationDeleted(String applicationId) {
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("DELETE")
                .applicationId(UUID.fromString(applicationId))
                .correlationId(getCorrelationId())
                .traceId(getTraceId())
                .timestamp(java.time.Instant.now())
                .version("1.0")
                .build();

        Message<ApplicationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka_messageKey", applicationId)
                .setHeader("correlation_id", event.getCorrelationId())
                .setHeader("trace_id", event.getTraceId())
                .build();

        kafkaTemplate.send("loan.applications", message);
        log.info("Published application deleted event {} with correlationId {}", 
                applicationId, event.getCorrelationId());
    }
}
