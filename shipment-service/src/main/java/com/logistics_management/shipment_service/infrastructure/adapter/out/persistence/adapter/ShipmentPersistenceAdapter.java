package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.adapter;

import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.entity.ShipmentEntity;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.mapper.ShipmentPersistenceMapper;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.repository.ShipmentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentPersistenceAdapter implements ShipmentPersistencePort {
    private final ShipmentJpaRepository repository;
    private final ShipmentPersistenceMapper mapper;

    @Override
    public Shipment save(Shipment shipment) {
        log.info("Persistencia: guardando envio en PostgreSQL. "
                        + "shipmentId={} trackingNumber={} status={}",
                shipment.getId(), shipment.getTrackingNumber(), shipment.getStatus());
        Shipment saved = mapper.toDomain(repository.save(mapper.toEntity(shipment)));
        log.info("Persistencia: envio guardado en PostgreSQL. shipmentId={}", saved.getId());
        return saved;
    }

    @Override
    public Optional<Shipment> findById(UUID id) {
        log.info("Persistencia: consultando envio en PostgreSQL. shipmentId={}", id);
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Shipment> findByIdempotencyKey(String idempotencyKey) {
        Optional<Shipment> result = repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
        log.info("Persistencia e idempotencia: consulta terminada. "
                        + "idempotencyKey={} existe={}", idempotencyKey, result.isPresent());
        return result;
    }

    @Override
    public ShipmentPage findAll(int page, int size) {
        log.info("Persistencia: consultando pagina de envios. page={} size={}", page, size);
        return mapPage(repository.findAll(PageRequest.of(page, size)));
    }

    @Override
    public ShipmentPage findByStatus(ShipmentStatus status, int page, int size) {
        log.info("Persistencia: consultando envios por estado. "
                + "status={} page={} size={}", status, page, size);
        return mapPage(repository.findByStatus(status, PageRequest.of(page, size)));
    }

    @Override
    public boolean existsByTrackingNumber(String trackingNumber) {
        return repository.existsByTrackingNumber(trackingNumber);
    }

    private ShipmentPage mapPage(Page<ShipmentEntity> page) {
        return new ShipmentPage(page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
