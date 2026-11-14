package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.adapter;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.entity.ShipmentEntity;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.mapper.ShipmentPersistenceMapper;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.repository.ShipmentJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentPersistenceAdapterTest {
    @Mock ShipmentJpaRepository repository;
    @Mock ShipmentPersistenceMapper mapper;
    @InjectMocks ShipmentPersistenceAdapter adapter;

    @Test
    void savesAndMapsShipment() {
        Shipment input = shipment(ShipmentStatus.CREATED);
        ShipmentEntity entity = new ShipmentEntity();
        ShipmentEntity storedEntity = new ShipmentEntity();
        Shipment stored = shipment(ShipmentStatus.CREATED);
        when(mapper.toEntity(input)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(storedEntity);
        when(mapper.toDomain(storedEntity)).thenReturn(stored);

        assertThat(adapter.save(input)).isSameAs(stored);
    }

    @Test
    void findsByIdAndIdempotencyKeyIncludingMissingValues() {
        UUID id = UUID.randomUUID();
        ShipmentEntity entity = new ShipmentEntity();
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findByIdempotencyKey("present")).thenReturn(Optional.of(entity));
        when(repository.findByIdempotencyKey("missing")).thenReturn(Optional.empty());
        when(mapper.toDomain(entity)).thenReturn(shipment);

        assertThat(adapter.findById(id)).contains(shipment);
        assertThat(adapter.findByIdempotencyKey("present")).contains(shipment);
        assertThat(adapter.findByIdempotencyKey("missing")).isEmpty();
    }

    @Test
    void mapsUnfilteredAndFilteredPages() {
        ShipmentEntity entity = new ShipmentEntity();
        Shipment shipment = shipment(ShipmentStatus.DISPATCHED);
        PageRequest pageable = PageRequest.of(1, 2);
        PageImpl<ShipmentEntity> entities = new PageImpl<>(List.of(entity), pageable, 5);
        when(repository.findAll(pageable)).thenReturn(entities);
        when(repository.findByStatus(ShipmentStatus.DISPATCHED, pageable)).thenReturn(entities);
        when(mapper.toDomain(entity)).thenReturn(shipment);

        var all = adapter.findAll(1, 2);
        var filtered = adapter.findByStatus(ShipmentStatus.DISPATCHED, 1, 2);

        assertThat(all.getContent()).containsExactly(shipment);
        assertThat(all.getPage()).isEqualTo(1);
        assertThat(all.getSize()).isEqualTo(2);
        assertThat(all.getTotalElements()).isEqualTo(5);
        assertThat(all.getTotalPages()).isEqualTo(3);
        assertThat(filtered.getContent()).containsExactly(shipment);
    }

    @Test
    void checksTrackingNumberExistence() {
        when(repository.existsByTrackingNumber("TRK-1")).thenReturn(true);

        assertThat(adapter.existsByTrackingNumber("TRK-1")).isTrue();
        verify(repository).existsByTrackingNumber("TRK-1");
    }

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder().id(UUID.randomUUID()).trackingNumber("TRK-1").status(status).build();
    }
}
