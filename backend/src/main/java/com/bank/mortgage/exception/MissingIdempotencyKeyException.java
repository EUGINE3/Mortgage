package com.bank.mortgage.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException(String headerName) {
        super("Missing required header: " + headerName);
    }
}
