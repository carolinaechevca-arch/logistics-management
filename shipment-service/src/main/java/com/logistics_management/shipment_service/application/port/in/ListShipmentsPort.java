package com.logistics_management.shipment_service.application.port.in;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;

public interface ListShipmentsPort {
    ShipmentPage list(int page, int size, ShipmentStatus status);
}
