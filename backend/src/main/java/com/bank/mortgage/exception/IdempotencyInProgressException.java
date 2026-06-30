package com.bank.mortgage.exception;

public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException() {
        super("Request is being processed, please try again shortly");
    }
}
