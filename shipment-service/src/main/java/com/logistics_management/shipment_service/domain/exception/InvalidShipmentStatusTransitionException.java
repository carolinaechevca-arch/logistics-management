package com.logistics_management.shipment_service.domain.exception;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;

public class InvalidShipmentStatusTransitionException extends RuntimeException {
    public InvalidShipmentStatusTransitionException(ShipmentStatus current, ShipmentStatus target) {
        super("Invalid shipment status transition from " + current + " to " + target);
    }
}
