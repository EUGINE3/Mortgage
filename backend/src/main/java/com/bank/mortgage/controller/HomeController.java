package com.bank.mortgage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of(
                "message", "Welcome to Mortgage Service API",
                "version", "1.0.0",
                "status", "Running",
                "documentation", "Available at /swagger-ui.html",
                "h2-console", "Available at /h2-console");
    }

    @GetMapping
    public Map<String, Object> root() {
        return Map.of(
                "message", "Welcome to Mortgage Service API",
                "version", "1.0.0",
                "status", "Running",
                "documentation", "Available at /swagger-ui.html",
                "h2-console", "Available at /h2-console");
    }
}
