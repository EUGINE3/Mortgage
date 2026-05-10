package com.bank.mortgage.exception;

import com.bank.mortgage.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException ex
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .errorCode("VALIDATION_ERROR")
                .detail(ex.getMessage())
                .build();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            NotFoundException ex
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .errorCode("NOT_FOUND")
                .detail(ex.getMessage())
                .build();
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(
            UnauthorizedException ex
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .errorCode("UNAUTHORIZED")
                .detail(ex.getMessage())
                .build();
    }
}
