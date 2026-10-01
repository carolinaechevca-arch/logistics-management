package com.logistics_management.shipment_service.application.port.in;

import com.logistics_management.shipment_service.domain.model.Shipment;

import java.util.UUID;

public interface CancelShipmentPort {
    Shipment cancel(UUID id);
}
