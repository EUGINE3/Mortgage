package com.bank.mortgage.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
    }

    @Test
    void acquireLock_shouldReturnTrueWhenRedisSetsKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("key-1"),
                eq(IdempotencyService.PROCESSING_VALUE),
                any(Duration.class)))
                .thenReturn(true);

        assertThat(idempotencyService.acquireLock("key-1", Duration.ofMinutes(2))).isTrue();
    }

    @Test
    void saveResponse_shouldSerializeAndStoreWithTtl() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        var response = com.bank.mortgage.dto.response.ApplicationResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .loanAmount(BigDecimal.valueOf(100000))
                .tenureMonths(60)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        idempotencyService.saveResponse("key-1", response, Duration.ofHours(24));

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("key-1"), jsonCaptor.capture(), eq(Duration.ofHours(24)));

        assertThat(jsonCaptor.getValue()).contains("\"status\":\"PENDING\"");
    }

    @Test
    void getResponse_shouldDeserializeCachedValue() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String json = """
                {"id":"11111111-1111-1111-1111-111111111111","status":"PENDING",
                "loanAmount":100000,"tenureMonths":60,"createdAt":"2024-01-01T00:00:00Z"}
                """;

        when(valueOperations.get("key-1")).thenReturn(json);

        var result = idempotencyService.getResponse(
                "key-1",
                com.bank.mortgage.dto.response.ApplicationResponse.class);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getLoanAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void getResponse_shouldReturnNullWhenProcessing() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key-1")).thenReturn(IdempotencyService.PROCESSING_VALUE);

        assertThat(idempotencyService.getResponse(
                "key-1",
                com.bank.mortgage.dto.response.ApplicationResponse.class)).isNull();
    }

    @Test
    void isProcessing_shouldDetectProcessingMarker() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key-1")).thenReturn(IdempotencyService.PROCESSING_VALUE);

        assertThat(idempotencyService.isProcessing("key-1")).isTrue();
    }

    @Test
    void releaseLock_shouldDeleteRedisKey() {
        idempotencyService.releaseLock("key-1");

        verify(redisTemplate).delete("key-1");
    }
}
