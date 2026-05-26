package com.bank.mortgage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long TIME_WINDOW_MS = 60 * 1000; // 1 minute

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientId(request);
        long now = System.currentTimeMillis();

        ConcurrentLinkedQueue<Long> timestamps = requestTimestamps.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>());

        // Remove old timestamps outside the time window
        timestamps.removeIf(timestamp -> now - timestamp > TIME_WINDOW_MS);

        // Check if limit exceeded
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            log.warn("Rate limit exceeded for client: {}", clientId);
            return false;
        }

        timestamps.add(now);
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_MINUTE - timestamps.size()));

        return true;
    }

    private String getClientId(HttpServletRequest request) {
        // Use IP address or authenticated user as client ID
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}
