package com.logistics_management.shipment_service.domain.model;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.exception.InvalidShipmentStatusTransitionException;
import com.logistics_management.shipment_service.domain.exception.ShipmentCannotBeCancelledException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    private UUID id;
    private String idempotencyKey;
    private String trackingNumber;
    private String customerName;
    private String customerEmail;
    private String origin;
    private String destination;
    private String description;
    private ShipmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void transitionTo(ShipmentStatus target, LocalDateTime transitionTime) {
        boolean valid = status == ShipmentStatus.CREATED && target == ShipmentStatus.DISPATCHED
                || status == ShipmentStatus.DISPATCHED && target == ShipmentStatus.IN_TRANSIT
                || status == ShipmentStatus.IN_TRANSIT && target == ShipmentStatus.DELIVERED;
        if (!valid) {
            throw new InvalidShipmentStatusTransitionException(status, target);
        }
        status = target;
        updatedAt = transitionTime;
    }

    public void cancel(LocalDateTime transitionTime) {
        if (status != ShipmentStatus.CREATED) {
            throw new ShipmentCannotBeCancelledException(status);
        }
        status = ShipmentStatus.CANCELLED;
        updatedAt = transitionTime;
    }
}
