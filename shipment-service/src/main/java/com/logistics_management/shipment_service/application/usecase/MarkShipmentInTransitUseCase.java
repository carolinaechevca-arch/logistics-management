package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.MarkShipmentInTransitPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.exception.ShipmentNotFoundException;
import com.logistics_management.shipment_service.domain.model.Shipment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class MarkShipmentInTransitUseCase implements MarkShipmentInTransitPort {
    private final ShipmentPersistencePort persistencePort;
    private final ShipmentEventPublisherPort eventPublisherPort;
    private final Clock clock;

    @Override
    public Shipment markInTransit(UUID id) {
        Shipment shipment = persistencePort.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
        LocalDateTime now = LocalDateTime.now(clock);
        shipment.transitionTo(ShipmentStatus.IN_TRANSIT, now);
        Shipment saved = persistencePort.save(shipment);
        log.info("shipment marked in transit shipmentId={} trackingNumber={} status={}",
                saved.getId(), saved.getTrackingNumber(), saved.getStatus());
        eventPublisherPort.publish(ShipmentEventFactory.create(saved, ShipmentEventType.SHIPMENT_IN_TRANSIT, now));
        return saved;
    }
}
