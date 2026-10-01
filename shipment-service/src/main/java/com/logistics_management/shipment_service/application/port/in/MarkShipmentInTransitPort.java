package com.logistics_management.shipment_service.application.port.in;

import com.logistics_management.shipment_service.domain.model.Shipment;

import java.util.UUID;

public interface MarkShipmentInTransitPort {
    Shipment markInTransit(UUID id);
}
