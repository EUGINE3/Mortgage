package com.bank.mortgage.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.idempotency.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyService {

    static final String PROCESSING_VALUE = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public boolean acquireLock(String key, Duration lockTtl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, PROCESSING_VALUE, lockTtl);
        return Boolean.TRUE.equals(acquired);
    }

    public void saveResponse(String key, Object response, Duration ttl) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(response);
        redisTemplate.opsForValue().set(key, json, ttl);
    }

    public <T> T getResponse(String key, Class<T> type) throws JsonProcessingException {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || PROCESSING_VALUE.equals(value)) {
            return null;
        }
        return objectMapper.readValue(value, type);
    }

    public boolean isProcessing(String key) {
        return PROCESSING_VALUE.equals(redisTemplate.opsForValue().get(key));
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }
}
