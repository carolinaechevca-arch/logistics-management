package com.logistics_management.shipment_service.domain.exception;

public class DuplicateTrackingNumberException extends RuntimeException {
    public DuplicateTrackingNumberException(String trackingNumber) {
        super("Tracking number already exists: " + trackingNumber);
    }
}
