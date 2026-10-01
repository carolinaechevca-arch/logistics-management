package com.logistics_management.shipment_service.application.port.in;

public record CreateShipmentCommand(
        String idempotencyKey,
        String customerName,
        String customerEmail,
        String origin,
        String destination,
        String description) {
}
