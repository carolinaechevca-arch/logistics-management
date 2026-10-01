package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.repository;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.entity.ShipmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface ShipmentJpaRepository extends JpaRepository<ShipmentEntity, UUID> {
    Page<ShipmentEntity> findByStatus(ShipmentStatus status, Pageable pageable);
    Optional<ShipmentEntity> findByIdempotencyKey(String idempotencyKey);
    boolean existsByTrackingNumber(String trackingNumber);
}
