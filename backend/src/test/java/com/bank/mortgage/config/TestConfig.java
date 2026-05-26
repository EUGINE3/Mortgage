package com.bank.mortgage.config;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.events.EventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EventPublisher testEventPublisher() {

        return new EventPublisher() {

            @Override
            public void publishApplicationCreated(Application application) {
                // no-op
            }

            @Override
            public void publishApplicationUpdated(Application application) {
                // no-op
            }

            @Override
            public void publishApplicationDeleted(String applicationId) {
                // no-op
            }
        };
    }
}
