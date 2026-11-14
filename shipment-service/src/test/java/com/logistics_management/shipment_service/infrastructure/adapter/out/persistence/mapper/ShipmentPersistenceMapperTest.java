package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.mapper;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentPersistenceMapperTest {
    private final ShipmentPersistenceMapper mapper = new ShipmentPersistenceMapper();

    @Test
    void mapsAllFieldsBetweenDomainAndEntity() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 13, 8, 0);
        LocalDateTime updatedAt = createdAt.plusMinutes(30);
        Shipment shipment = Shipment.builder().id(id).idempotencyKey("idem-1").trackingNumber("TRK-1")
                .customerName("Carolina").customerEmail("carolina@example.com")
                .origin("Bogota").destination("Medellin").description("Caja")
                .status(ShipmentStatus.DISPATCHED).createdAt(createdAt).updatedAt(updatedAt).build();

        var entity = mapper.toEntity(shipment);
        var restored = mapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(entity.getStatus()).isEqualTo(ShipmentStatus.DISPATCHED);
        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getTrackingNumber()).isEqualTo("TRK-1");
        assertThat(restored.getCustomerName()).isEqualTo("Carolina");
        assertThat(restored.getCustomerEmail()).isEqualTo("carolina@example.com");
        assertThat(restored.getOrigin()).isEqualTo("Bogota");
        assertThat(restored.getDestination()).isEqualTo("Medellin");
        assertThat(restored.getDescription()).isEqualTo("Caja");
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
