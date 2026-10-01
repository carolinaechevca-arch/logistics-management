package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException;
import com.logistics_management.shipment_service.domain.exception.InvalidShipmentStatusTransitionException;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentUseCasesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    @Mock
    private ShipmentPersistencePort persistencePort;
    @Mock
    private ShipmentEventPublisherPort publisherPort;

    @Test
    void createsShipmentAndPublishesCreatedEvent() {
        when(persistencePort.findByIdempotencyKey("create-1")).thenReturn(Optional.empty());
        when(persistencePort.existsByTrackingNumber(any())).thenReturn(false);
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        Shipment shipment = useCase.create(new CreateShipmentCommand(
                "create-1", "Carolina", "carolina@email.com", "Medellín", "Bogotá", "Portátil"));

        assertThat(shipment.getId()).isNotNull();
        assertThat(shipment.getTrackingNumber()).startsWith("SHP-");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        ArgumentCaptor<ShipmentEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(publisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(ShipmentEventType.SHIPMENT_CREATED);
    }

    @Test
    void returnsExistingShipmentWithoutSavingOrPublishingForIdempotentReplay() {
        Shipment existing = Shipment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("create-2")
                .customerName("Carolina")
                .customerEmail("carolina@email.com")
                .origin("Medellín")
                .destination("Bogotá")
                .description("Portátil")
                .build();
        when(persistencePort.findByIdempotencyKey("create-2")).thenReturn(Optional.of(existing));
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        Shipment replayed = useCase.create(new CreateShipmentCommand(
                "create-2", "Carolina", "carolina@email.com", "Medellín", "Bogotá", "Portátil"));

        assertThat(replayed).isSameAs(existing);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void rejectsIdempotencyKeyReusedWithDifferentPayload() {
        Shipment existing = Shipment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("create-3")
                .customerName("Carolina")
                .customerEmail("carolina@email.com")
                .origin("Medellín")
                .destination("Bogotá")
                .build();
        when(persistencePort.findByIdempotencyKey("create-3")).thenReturn(Optional.of(existing));
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        assertThatThrownBy(() -> useCase.create(new CreateShipmentCommand(
                "create-3", "Otra persona", "carolina@email.com", "Medellín", "Bogotá", null)))
                .isInstanceOf(IdempotencyKeyConflictException.class);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void rejectsCreatedToDeliveredWithoutSavingOrPublishing() {
        Shipment created = Shipment.builder().id(UUID.randomUUID()).status(ShipmentStatus.CREATED).build();
        when(persistencePort.findById(created.getId())).thenReturn(Optional.of(created));
        DeliverShipmentUseCase useCase = new DeliverShipmentUseCase(persistencePort, publisherPort, CLOCK);

        assertThatThrownBy(() -> useCase.deliver(created.getId()))
                .isInstanceOf(InvalidShipmentStatusTransitionException.class);
        assertThat(created.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }
}
