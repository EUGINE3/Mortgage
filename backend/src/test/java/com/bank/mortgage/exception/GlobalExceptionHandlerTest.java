package com.bank.mortgage.exception;

import com.bank.mortgage.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_shouldReturnNotFoundErrorResponse() {
        ErrorResponse response = handler.handleNotFound(
                new NotFoundException("Application not found"));

        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getDetail()).isEqualTo("Application not found");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void handleUnauthorized_shouldReturnUnauthorizedErrorResponse() {
        ErrorResponse response = handler.handleUnauthorized(
                new UnauthorizedException("Invalid credentials"));

        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getDetail()).isEqualTo("Invalid credentials");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void handleMissingIdempotencyKey_shouldReturnBadRequest() {
        ErrorResponse response = handler.handleMissingIdempotencyKey(
                new MissingIdempotencyKeyException("X-Idempotency-Key"));

        assertThat(response.getErrorCode()).isEqualTo("MISSING_IDEMPOTENCY_KEY");
        assertThat(response.getDetail()).contains("X-Idempotency-Key");
    }

    @Test
    void handleIdempotencyInProgress_shouldReturnConflict() {
        ErrorResponse response = handler.handleIdempotencyInProgress(
                new IdempotencyInProgressException());

        assertThat(response.getErrorCode()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void handleValidation_shouldReturnValidationErrorResponse() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getMessage()).thenReturn("Validation failed for field 'loanAmount': must be positive");

        ErrorResponse response = handler.handleValidation(ex);

        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getDetail()).contains("loanAmount");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
