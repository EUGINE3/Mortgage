package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishApplicationCreated(Application application) {

        kafkaTemplate.send(
                "loan.applications",
                application.getId().toString(),
                application
        );

        log.info("Published application created event {}", application.getId());
    }
}
