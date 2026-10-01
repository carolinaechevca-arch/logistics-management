package com.logistics_management.shipment_service.domain.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(UUID id) {
        super("Shipment not found: " + id);
    }
}
