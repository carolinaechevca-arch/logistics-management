package com.logistics_management.shipment_service.application.port.in;

import com.logistics_management.shipment_service.domain.model.Shipment;

public interface CreateShipmentPort {
    Shipment create(CreateShipmentCommand command);
}
