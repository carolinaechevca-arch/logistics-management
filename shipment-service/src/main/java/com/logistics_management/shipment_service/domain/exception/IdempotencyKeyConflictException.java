package com.logistics_management.shipment_service.domain.exception;

public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency key was already used with a different request: " + idempotencyKey);
    }
}
