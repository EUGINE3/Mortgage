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
        log.info("Kafka is disabled. Event publishing skipped for application {}", application.getId());
    }
}
