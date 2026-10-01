package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.mapper;

import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.entity.ShipmentEntity;
import org.springframework.stereotype.Component;

@Component
public class ShipmentPersistenceMapper {
    public ShipmentEntity toEntity(Shipment shipment) {
        return new ShipmentEntity(
                shipment.getId(), shipment.getIdempotencyKey(), shipment.getTrackingNumber(), shipment.getCustomerName(),
                shipment.getCustomerEmail(), shipment.getOrigin(), shipment.getDestination(),
                shipment.getDescription(), shipment.getStatus(), shipment.getCreatedAt(), shipment.getUpdatedAt());
    }

    public Shipment toDomain(ShipmentEntity entity) {
        return Shipment.builder()
                .id(entity.getId())
                .idempotencyKey(entity.getIdempotencyKey())
                .trackingNumber(entity.getTrackingNumber())
                .customerName(entity.getCustomerName())
                .customerEmail(entity.getCustomerEmail())
                .origin(entity.getOrigin())
                .destination(entity.getDestination())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
