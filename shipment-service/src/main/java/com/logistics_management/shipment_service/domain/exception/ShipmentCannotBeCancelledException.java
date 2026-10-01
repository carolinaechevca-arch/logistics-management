package com.logistics_management.shipment_service.domain.exception;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;

public class ShipmentCannotBeCancelledException extends RuntimeException {
    public ShipmentCannotBeCancelledException(ShipmentStatus status) {
        super("Shipment cannot be cancelled while its status is " + status);
    }
}
