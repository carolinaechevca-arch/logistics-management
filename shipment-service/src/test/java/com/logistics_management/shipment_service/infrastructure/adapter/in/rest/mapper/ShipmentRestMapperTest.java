package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.mapper;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.CreateShipmentRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentRestMapperTest {
    private final ShipmentRestMapper mapper = new ShipmentRestMapper();

    @Test
    void mapsCreateRequestToCommand() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                "Carolina", "carolina@example.com", "Bogota", "Cali", "Documentos");

        var command = mapper.toCommand("idem-10", request);

        assertThat(command.idempotencyKey()).isEqualTo("idem-10");
        assertThat(command.customerName()).isEqualTo("Carolina");
        assertThat(command.customerEmail()).isEqualTo("carolina@example.com");
        assertThat(command.origin()).isEqualTo("Bogota");
        assertThat(command.destination()).isEqualTo("Cali");
        assertThat(command.description()).isEqualTo("Documentos");
    }

    @Test
    void mapsDomainShipmentAndPageToRestResponses() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 13, 10, 0);
        LocalDateTime updatedAt = createdAt.plusHours(1);
        UUID id = UUID.randomUUID();
        Shipment shipment = Shipment.builder().id(id).trackingNumber("TRK-100")
                .customerName("Carolina").customerEmail("carolina@example.com")
                .origin("Bogota").destination("Cali").description("Documentos")
                .status(ShipmentStatus.IN_TRANSIT).createdAt(createdAt).updatedAt(updatedAt).build();

        var response = mapper.toResponse(shipment);
        var page = mapper.toResponse(new ShipmentPage(List.of(shipment), 2, 10, 21, 3));

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(page.content()).containsExactly(response);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(3);
    }
}
