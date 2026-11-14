package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.exception.DuplicateTrackingNumberException;
import com.logistics_management.shipment_service.domain.exception.InvalidIdempotencyKeyException;
import com.logistics_management.shipment_service.domain.exception.InvalidShipmentStatusTransitionException;
import com.logistics_management.shipment_service.domain.exception.ShipmentCannotBeCancelledException;
import com.logistics_management.shipment_service.domain.exception.ShipmentNotFoundException;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentEvent;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentUseCasesCoverageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ShipmentPersistencePort persistencePort;
    @Mock
    private ShipmentEventPublisherPort publisherPort;

    @Test
    void rejectsEveryInvalidIdempotencyKeyVariant() {
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        assertThatThrownBy(() -> useCase.create(command(null)))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        assertThatThrownBy(() -> useCase.create(command(" ")))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        assertThatThrownBy(() -> useCase.create(command("x".repeat(129))))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void rejectsIdempotentReplayWhenAnyPayloadFieldDiffers() {
        Shipment existing = shipment(ShipmentStatus.CREATED);
        existing.setIdempotencyKey("same-key");
        when(persistencePort.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        assertThatThrownBy(() -> useCase.create(new CreateShipmentCommand("same-key", "Carolina",
                "otra@email.com", "Medellín", "Bogotá", "Portátil")))
                .isInstanceOf(com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException.class);
        assertThatThrownBy(() -> useCase.create(new CreateShipmentCommand("same-key", "Carolina",
                "carolina@email.com", "Cali", "Bogotá", "Portátil")))
                .isInstanceOf(com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException.class);
        assertThatThrownBy(() -> useCase.create(new CreateShipmentCommand("same-key", "Carolina",
                "carolina@email.com", "Medellín", "Cali", "Portátil")))
                .isInstanceOf(com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException.class);
        assertThatThrownBy(() -> useCase.create(new CreateShipmentCommand("same-key", "Carolina",
                "carolina@email.com", "Medellín", "Bogotá", "Documento")))
                .isInstanceOf(com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException.class);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void failsWhenUniqueTrackingNumberCannotBeGenerated() {
        when(persistencePort.findByIdempotencyKey("tracking-exhausted")).thenReturn(Optional.empty());
        when(persistencePort.existsByTrackingNumber(any())).thenReturn(true);
        CreateShipmentUseCase useCase = new CreateShipmentUseCase(persistencePort, publisherPort, CLOCK);

        assertThatThrownBy(() -> useCase.create(command("tracking-exhausted")))
                .isInstanceOf(DuplicateTrackingNumberException.class);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void getsExistingShipment() {
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        when(persistencePort.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        Shipment result = new GetShipmentUseCase(persistencePort).getById(shipment.getId());

        assertThat(result).isSameAs(shipment);
    }

    @Test
    void getFailsWhenShipmentDoesNotExist() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new GetShipmentUseCase(persistencePort).getById(id))
                .isInstanceOf(ShipmentNotFoundException.class);
    }

    @Test
    void listsAllShipmentsWhenStatusIsAbsent() {
        ShipmentPage page = new ShipmentPage(List.of(shipment(ShipmentStatus.CREATED)), 0, 10, 1, 1);
        when(persistencePort.findAll(0, 10)).thenReturn(page);

        ShipmentPage result = new ListShipmentsUseCase(persistencePort).list(0, 10, null);

        assertThat(result).isSameAs(page);
        verify(persistencePort).findAll(0, 10);
    }

    @Test
    void listsShipmentsFilteredByStatus() {
        ShipmentPage page = new ShipmentPage(List.of(shipment(ShipmentStatus.DELIVERED)), 1, 5, 1, 1);
        when(persistencePort.findByStatus(ShipmentStatus.DELIVERED, 1, 5)).thenReturn(page);

        ShipmentPage result = new ListShipmentsUseCase(persistencePort)
                .list(1, 5, ShipmentStatus.DELIVERED);

        assertThat(result).isSameAs(page);
        verify(persistencePort).findByStatus(ShipmentStatus.DELIVERED, 1, 5);
    }

    @Test
    void dispatchesCreatedShipmentAndPublishesEvent() {
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        prepareSave(shipment);

        Shipment result = new DispatchShipmentUseCase(persistencePort, publisherPort, CLOCK)
                .dispatch(shipment.getId());

        assertTransition(result, ShipmentStatus.DISPATCHED, ShipmentEventType.SHIPMENT_DISPATCHED);
    }

    @Test
    void marksDispatchedShipmentInTransitAndPublishesEvent() {
        Shipment shipment = shipment(ShipmentStatus.DISPATCHED);
        prepareSave(shipment);

        Shipment result = new MarkShipmentInTransitUseCase(persistencePort, publisherPort, CLOCK)
                .markInTransit(shipment.getId());

        assertTransition(result, ShipmentStatus.IN_TRANSIT, ShipmentEventType.SHIPMENT_IN_TRANSIT);
    }

    @Test
    void deliversInTransitShipmentAndPublishesEvent() {
        Shipment shipment = shipment(ShipmentStatus.IN_TRANSIT);
        prepareSave(shipment);

        Shipment result = new DeliverShipmentUseCase(persistencePort, publisherPort, CLOCK)
                .deliver(shipment.getId());

        assertTransition(result, ShipmentStatus.DELIVERED, ShipmentEventType.SHIPMENT_DELIVERED);
    }

    @Test
    void cancelsCreatedShipmentAndPublishesEvent() {
        Shipment shipment = shipment(ShipmentStatus.CREATED);
        prepareSave(shipment);

        Shipment result = new CancelShipmentUseCase(persistencePort, publisherPort, CLOCK)
                .cancel(shipment.getId());

        assertTransition(result, ShipmentStatus.CANCELLED, ShipmentEventType.SHIPMENT_CANCELLED);
    }

    @Test
    void rejectsCancellationOutsideCreatedState() {
        Shipment shipment = shipment(ShipmentStatus.DISPATCHED);
        when(persistencePort.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> new CancelShipmentUseCase(persistencePort, publisherPort, CLOCK)
                .cancel(shipment.getId()))
                .isInstanceOf(ShipmentCannotBeCancelledException.class);
        verify(persistencePort, never()).save(any());
        verify(publisherPort, never()).publish(any());
    }

    @Test
    void rejectsInvalidTargetsFromIntermediateStates() {
        Shipment dispatched = shipment(ShipmentStatus.DISPATCHED);
        Shipment inTransit = shipment(ShipmentStatus.IN_TRANSIT);

        assertThatThrownBy(() -> dispatched.transitionTo(ShipmentStatus.DELIVERED, LocalDateTime.now(CLOCK)))
                .isInstanceOf(InvalidShipmentStatusTransitionException.class);
        assertThatThrownBy(() -> inTransit.transitionTo(ShipmentStatus.DISPATCHED, LocalDateTime.now(CLOCK)))
                .isInstanceOf(InvalidShipmentStatusTransitionException.class);
    }

    @Test
    void stateChangesFailWhenShipmentDoesNotExist() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new DispatchShipmentUseCase(persistencePort, publisherPort, CLOCK).dispatch(id))
                .isInstanceOf(ShipmentNotFoundException.class);
        assertThatThrownBy(() -> new MarkShipmentInTransitUseCase(persistencePort, publisherPort, CLOCK).markInTransit(id))
                .isInstanceOf(ShipmentNotFoundException.class);
        assertThatThrownBy(() -> new DeliverShipmentUseCase(persistencePort, publisherPort, CLOCK).deliver(id))
                .isInstanceOf(ShipmentNotFoundException.class);
        assertThatThrownBy(() -> new CancelShipmentUseCase(persistencePort, publisherPort, CLOCK).cancel(id))
                .isInstanceOf(ShipmentNotFoundException.class);
    }

    private void prepareSave(Shipment shipment) {
        when(persistencePort.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(persistencePort.save(shipment)).thenReturn(shipment);
    }

    private void assertTransition(Shipment result, ShipmentStatus status, ShipmentEventType eventType) {
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        ArgumentCaptor<ShipmentEvent> captor = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(publisherPort).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(eventType);
        assertThat(captor.getValue().getShipmentId()).isEqualTo(result.getId());
    }

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("key-1")
                .trackingNumber("SHP-123")
                .customerName("Carolina")
                .customerEmail("carolina@email.com")
                .origin("Medellín")
                .destination("Bogotá")
                .description("Portátil")
                .status(status)
                .createdAt(LocalDateTime.now(CLOCK).minusHours(1))
                .updatedAt(LocalDateTime.now(CLOCK).minusHours(1))
                .build();
    }

    private CreateShipmentCommand command(String key) {
        return new CreateShipmentCommand(key, "Carolina", "carolina@email.com",
                "Medellín", "Bogotá", "Portátil");
    }
}
