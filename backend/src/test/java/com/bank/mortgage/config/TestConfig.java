package com.bank.mortgage.config;

import com.bank.mortgage.events.EventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EventPublisher testEventPublisher() {
        return application -> {
            // No-op implementation for testing
        };
    }
}
