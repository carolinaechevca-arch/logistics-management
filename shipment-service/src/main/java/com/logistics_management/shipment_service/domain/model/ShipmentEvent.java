package com.logistics_management.shipment_service.domain.model;

import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentEvent {
    private UUID eventId;
    private ShipmentEventType eventType;
    private UUID shipmentId;
    private String trackingNumber;
    private String customerName;
    private String customerEmail;
    private String origin;
    private String destination;
    private ShipmentStatus status;
    private LocalDateTime occurredAt;
}
