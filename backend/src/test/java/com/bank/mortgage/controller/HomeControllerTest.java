package com.bank.mortgage.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void homeShouldReturnWelcomePayload() {
        Map<String, Object> response = homeController.home();

        assertThat(response)
                .containsEntry("message", "Welcome to Mortgage Service API")
                .containsEntry("version", "1.0.0")
                .containsEntry("status", "Running")
                .containsEntry("documentation", "Available at /swagger-ui.html")
                .containsEntry("h2-console", "Available at /h2-console");
    }

    @Test
    void rootShouldReturnWelcomePayload() {
        Map<String, Object> response = homeController.root();

        assertThat(response)
                .containsEntry("message", "Welcome to Mortgage Service API")
                .containsEntry("version", "1.0.0")
                .containsEntry("status", "Running")
                .containsEntry("documentation", "Available at /swagger-ui.html")
                .containsEntry("h2-console", "Available at /h2-console");
    }
}
