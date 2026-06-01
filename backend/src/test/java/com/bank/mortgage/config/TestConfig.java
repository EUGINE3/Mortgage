package com.bank.mortgage.config;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.events.EventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity
public class TestConfig {

    /**
     * Disable real event publishing during tests
     * (prevents async side effects, messaging, etc.)
     */
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

    /**
     * TEST SECURITY OVERRIDE
     *
     * Disables CSRF and permits public access to home endpoint,
     * while enforcing role-based access through @PreAuthorize annotations
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/home").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
