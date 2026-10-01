package com.logistics_management.shipment_service.domain.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key header is required and must not be blank");
    }
}
