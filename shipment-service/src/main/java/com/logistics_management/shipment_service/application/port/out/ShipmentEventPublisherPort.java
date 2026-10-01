package com.logistics_management.shipment_service.application.port.out;

import com.logistics_management.shipment_service.domain.model.ShipmentEvent;

public interface ShipmentEventPublisherPort {
    void publish(ShipmentEvent event);
}
