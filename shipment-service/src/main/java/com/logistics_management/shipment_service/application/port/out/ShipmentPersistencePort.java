package com.logistics_management.shipment_service.application.port.out;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentPersistencePort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(UUID id);
    Optional<Shipment> findByIdempotencyKey(String idempotencyKey);
    ShipmentPage findAll(int page, int size);
    ShipmentPage findByStatus(ShipmentStatus status, int page, int size);
    boolean existsByTrackingNumber(String trackingNumber);
}
