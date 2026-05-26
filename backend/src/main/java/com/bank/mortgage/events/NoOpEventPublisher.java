package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "kafka.enabled",
        havingValue = "false"
)
@Slf4j
public class NoOpEventPublisher implements EventPublisher {

    @Override
    public void publishApplicationCreated(Application application) {
        log.debug("Kafka is disabled. Event publishing skipped for application created: {}", application.getId());
    }

    @Override
    public void publishApplicationUpdated(Application application) {
        log.debug("Kafka is disabled. Event publishing skipped for application updated: {}", application.getId());
    }

    @Override
    public void publishApplicationDeleted(String applicationId) {
        log.debug("Kafka is disabled. Event publishing skipped for application deleted: {}", applicationId);
    }
}
