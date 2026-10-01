package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.ListShipmentsPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ListShipmentsUseCase implements ListShipmentsPort {
    private final ShipmentPersistencePort persistencePort;

    @Override
    public ShipmentPage list(int page, int size, ShipmentStatus status) {
        log.info("shipment list requested page={} size={} status={}", page, size, status);
        ShipmentPage result = status == null
                ? persistencePort.findAll(page, size)
                : persistencePort.findByStatus(status, page, size);
        log.info("shipment list completed page={} returned={} totalElements={}",
                result.getPage(), result.getContent().size(), result.getTotalElements());
        return result;
    }
}
