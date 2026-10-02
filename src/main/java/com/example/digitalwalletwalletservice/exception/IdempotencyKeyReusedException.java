package com.example.digitalwalletwalletservice.exception;

public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super("Idempotency-Key '%s' was already used with a different request payload".formatted(idempotencyKey));
    }
}
