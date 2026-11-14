package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.controller;

import com.logistics_management.shipment_service.application.port.in.CancelShipmentPort;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DeliverShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DispatchShipmentPort;
import com.logistics_management.shipment_service.application.port.in.GetShipmentPort;
import com.logistics_management.shipment_service.application.port.in.ListShipmentsPort;
import com.logistics_management.shipment_service.application.port.in.MarkShipmentInTransitPort;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.CreateShipmentRequest;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentPageResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.mapper.ShipmentRestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerUnitTest {
    @Mock CreateShipmentPort createShipmentPort;
    @Mock GetShipmentPort getShipmentPort;
    @Mock ListShipmentsPort listShipmentsPort;
    @Mock DispatchShipmentPort dispatchShipmentPort;
    @Mock MarkShipmentInTransitPort markShipmentInTransitPort;
    @Mock DeliverShipmentPort deliverShipmentPort;
    @Mock CancelShipmentPort cancelShipmentPort;
    @Mock ShipmentRestMapper mapper;
    @InjectMocks ShipmentController controller;

    @Test
    void createsShipmentAndReturnsCreatedResponse() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                "Carolina", "carolina@example.com", "Bogota", "Medellin", "Caja");
        CreateShipmentCommand command = new CreateShipmentCommand(
                "idem-1", "Carolina", "carolina@example.com", "Bogota", "Medellin", "Caja");
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        ShipmentResponse response = response(ShipmentStatus.CREATED);
        when(mapper.toCommand("idem-1", request)).thenReturn(command);
        when(createShipmentPort.create(command)).thenReturn(shipment);
        when(mapper.toResponse(shipment)).thenReturn(response);

        var result = controller.create("idem-1", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void getsShipmentById() {
        UUID id = UUID.randomUUID();
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        ShipmentResponse response = response(ShipmentStatus.CREATED);
        when(getShipmentPort.getById(id)).thenReturn(shipment);
        when(mapper.toResponse(shipment)).thenReturn(response);

        assertThat(controller.getById(id)).isSameAs(response);
    }

    @Test
    void listsShipmentsWithFilterAndPagination() {
        ShipmentPage page = new ShipmentPage(List.of(shipment(ShipmentStatus.DISPATCHED)), 1, 5, 6, 2);
        ShipmentPageResponse response = new ShipmentPageResponse(List.of(response(ShipmentStatus.DISPATCHED)), 1, 5, 6, 2);
        when(listShipmentsPort.list(1, 5, ShipmentStatus.DISPATCHED)).thenReturn(page);
        when(mapper.toResponse(page)).thenReturn(response);

        assertThat(controller.list(1, 5, ShipmentStatus.DISPATCHED)).isSameAs(response);
    }

    @Test
    void delegatesEveryStatusOperationToItsUseCase() {
        UUID id = UUID.randomUUID();
        Shipment dispatched = shipment(ShipmentStatus.DISPATCHED);
        Shipment inTransit = shipment(ShipmentStatus.IN_TRANSIT);
        Shipment delivered = shipment(ShipmentStatus.DELIVERED);
        Shipment cancelled = shipment(ShipmentStatus.CANCELLED);
        ShipmentResponse dispatchedResponse = response(ShipmentStatus.DISPATCHED);
        ShipmentResponse inTransitResponse = response(ShipmentStatus.IN_TRANSIT);
        ShipmentResponse deliveredResponse = response(ShipmentStatus.DELIVERED);
        ShipmentResponse cancelledResponse = response(ShipmentStatus.CANCELLED);
        when(dispatchShipmentPort.dispatch(id)).thenReturn(dispatched);
        when(markShipmentInTransitPort.markInTransit(id)).thenReturn(inTransit);
        when(deliverShipmentPort.deliver(id)).thenReturn(delivered);
        when(cancelShipmentPort.cancel(id)).thenReturn(cancelled);
        when(mapper.toResponse(dispatched)).thenReturn(dispatchedResponse);
        when(mapper.toResponse(inTransit)).thenReturn(inTransitResponse);
        when(mapper.toResponse(delivered)).thenReturn(deliveredResponse);
        when(mapper.toResponse(cancelled)).thenReturn(cancelledResponse);

        assertThat(controller.dispatch(id)).isSameAs(dispatchedResponse);
        assertThat(controller.markInTransit(id)).isSameAs(inTransitResponse);
        assertThat(controller.deliver(id)).isSameAs(deliveredResponse);
        assertThat(controller.cancel(id)).isSameAs(cancelledResponse);
        verify(dispatchShipmentPort).dispatch(id);
        verify(markShipmentInTransitPort).markInTransit(id);
        verify(deliverShipmentPort).deliver(id);
        verify(cancelShipmentPort).cancel(id);
    }

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder().id(UUID.randomUUID()).trackingNumber("TRK-001")
                .customerName("Carolina").customerEmail("carolina@example.com")
                .origin("Bogota").destination("Medellin").description("Caja")
                .status(status).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private ShipmentResponse response(ShipmentStatus status) {
        return new ShipmentResponse(UUID.randomUUID(), "TRK-001", "Carolina", "carolina@example.com",
                "Bogota", "Medellin", "Caja", status, LocalDateTime.now(), LocalDateTime.now());
    }
}
