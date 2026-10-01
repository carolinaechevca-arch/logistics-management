package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.GetShipmentPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.exception.ShipmentNotFoundException;
import com.logistics_management.shipment_service.domain.model.Shipment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class GetShipmentUseCase implements GetShipmentPort {
    private final ShipmentPersistencePort persistencePort;

    @Override
    public Shipment getById(UUID id) {
        log.info("Buscando envio. shipmentId={}", id);
        Shipment shipment = persistencePort.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
        log.info("Envio encontrado. shipmentId={} trackingNumber={} status={}",
                shipment.getId(), shipment.getTrackingNumber(), shipment.getStatus());
        return shipment;
    }
}
