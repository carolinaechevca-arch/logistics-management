package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String trackingNumber,
        String customerName,
        String customerEmail,
        String origin,
        String destination,
        String description,
        ShipmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
