package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
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

    private static final String TOPIC = "loan.applications";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private String correlationId() {
        return UUID.randomUUID().toString();
    }

    private String traceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void publishApplicationCreated(Application application) {

        ApplicationEvent event = ApplicationEvent.fromApplication(
                application,
                "CREATE",
                correlationId(),
                traceId()
        );

        send(application.getId().toString(), event);

        log.info("Published CREATE event for applicationId={}", application.getId());
    }

    @Override
    public void publishApplicationUpdated(Application application) {

        ApplicationEvent event = ApplicationEvent.fromApplication(
                application,
                "UPDATE",
                correlationId(),
                traceId()
        );

        send(application.getId().toString(), event);

        log.info("Published UPDATE event for applicationId={}", application.getId());
    }

    @Override
    public void publishApplicationDeleted(String applicationId) {

        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("DELETE")
                .applicationId(UUID.fromString(applicationId))
                .correlationId(correlationId())
                .traceId(traceId())
                .timestamp(Instant.now())
                .version("1.0")
                .build();

        send(applicationId, event);

        log.info("Published DELETE event for applicationId={}", applicationId);
    }

    /**
     * SINGLE RESPONSIBILITY SEND METHOD
     */
    private void send(String key, ApplicationEvent event) {
        kafkaTemplate.send(TOPIC, key, event);
    }
}
