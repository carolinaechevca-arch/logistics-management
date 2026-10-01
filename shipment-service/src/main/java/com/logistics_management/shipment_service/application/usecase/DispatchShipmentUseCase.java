package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.DispatchShipmentPort;
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
public class DispatchShipmentUseCase implements DispatchShipmentPort {
    private final ShipmentPersistencePort persistencePort;
    private final ShipmentEventPublisherPort eventPublisherPort;
    private final Clock clock;

    @Override
    public Shipment dispatch(UUID id) {
        log.info("Iniciando cambio de estado a DISPATCHED. shipmentId={}", id);
        Shipment shipment = persistencePort.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
        LocalDateTime now = LocalDateTime.now(clock);
        shipment.transitionTo(ShipmentStatus.DISPATCHED, now);
        Shipment saved = persistencePort.save(shipment);
        log.info("Envio despachado. shipmentId={} trackingNumber={} status={}",
                saved.getId(), saved.getTrackingNumber(), saved.getStatus());
        eventPublisherPort.publish(ShipmentEventFactory.create(saved, ShipmentEventType.SHIPMENT_DISPATCHED, now));
        return saved;
    }
}
