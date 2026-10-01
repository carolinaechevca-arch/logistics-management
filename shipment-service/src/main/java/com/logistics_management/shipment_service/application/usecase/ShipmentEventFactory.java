package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentEvent;

import java.time.LocalDateTime;
import java.util.UUID;

final class ShipmentEventFactory {
    private ShipmentEventFactory() {
    }

    static ShipmentEvent create(Shipment shipment, ShipmentEventType type, LocalDateTime occurredAt) {
        return ShipmentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(type)
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .customerName(shipment.getCustomerName())
                .customerEmail(shipment.getCustomerEmail())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .status(shipment.getStatus())
                .occurredAt(occurredAt)
                .build();
    }
}
