package com.logistics_management.notification_service.application.usecase;

import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.domain.enums.ShipmentEventType;
import com.logistics_management.notification_service.domain.enums.ShipmentStatus;
import com.logistics_management.notification_service.domain.exception.NotificationProcessingException;
import com.logistics_management.notification_service.domain.model.Notification;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessShipmentNotificationUseCaseTest {
    @Test
    void ignoresDuplicateEventAfterSuccessfulDelivery() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);
        ShipmentEvent event = validEvent("carolina@email.com");

        useCase.process(event);
        useCase.process(event);

        verify(sender, times(1)).send(any(Notification.class));
    }

    @Test
    void laboratoryEmailAlwaysFailsAndIsNotMarkedAsProcessed() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);
        ShipmentEvent event = validEvent("fail@email.com");

        assertThatThrownBy(() -> useCase.process(event)).isInstanceOf(NotificationProcessingException.class);
        assertThatThrownBy(() -> useCase.process(event)).isInstanceOf(NotificationProcessingException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void invalidEventFailsWithoutSendingMail() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);
        ShipmentEvent event = new ShipmentEvent(UUID.randomUUID(), null, UUID.randomUUID(), "SHP-1",
                "Carolina", "carolina@email.com", "Medellín", "Bogotá", ShipmentStatus.CREATED, LocalDateTime.now());

        assertThatThrownBy(() -> useCase.process(event)).isInstanceOf(NotificationProcessingException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void rejectsEveryMissingRequiredField() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.process(null))
                .isInstanceOf(NotificationProcessingException.class);
        assertThatThrownBy(() -> useCase.process(new ShipmentEvent(null, ShipmentEventType.SHIPMENT_CREATED,
                shipmentId, "SHP-1", "Carolina", "carolina@email.com", "Medellín", "Bogotá",
                ShipmentStatus.CREATED, LocalDateTime.now())))
                .isInstanceOf(NotificationProcessingException.class);
        assertThatThrownBy(() -> useCase.process(new ShipmentEvent(eventId, ShipmentEventType.SHIPMENT_CREATED,
                null, "SHP-1", "Carolina", "carolina@email.com", "Medellín", "Bogotá",
                ShipmentStatus.CREATED, LocalDateTime.now())))
                .isInstanceOf(NotificationProcessingException.class);
        assertThatThrownBy(() -> useCase.process(new ShipmentEvent(eventId, ShipmentEventType.SHIPMENT_CREATED,
                shipmentId, "SHP-1", "Carolina", null, "Medellín", "Bogotá",
                ShipmentStatus.CREATED, LocalDateTime.now())))
                .isInstanceOf(NotificationProcessingException.class);
        assertThatThrownBy(() -> useCase.process(new ShipmentEvent(eventId, ShipmentEventType.SHIPMENT_CREATED,
                shipmentId, "SHP-1", "Carolina", " ", "Medellín", "Bogotá",
                ShipmentStatus.CREATED, LocalDateTime.now())))
                .isInstanceOf(NotificationProcessingException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void createsNotificationForEveryStateChangeEvent() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);

        useCase.process(validEvent(ShipmentEventType.SHIPMENT_DISPATCHED));
        useCase.process(validEvent(ShipmentEventType.SHIPMENT_IN_TRANSIT));
        useCase.process(validEvent(ShipmentEventType.SHIPMENT_DELIVERED));
        useCase.process(validEvent(ShipmentEventType.SHIPMENT_CANCELLED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sender, times(4)).send(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getSubject)
                .containsExactly(
                        "Tu envío fue despachado",
                        "Tu envío está en tránsito",
                        "Tu envío fue entregado",
                        "Tu envío fue cancelado");
        assertThat(captor.getAllValues()).extracting(Notification::getBody)
                .allSatisfy(body -> assertThat(body).contains("Actualización del envío"));
    }

    @Test
    void replacesOptionalNullValuesWithEmptyText() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        ProcessShipmentNotificationUseCase useCase = new ProcessShipmentNotificationUseCase(sender);
        ShipmentEvent event = new ShipmentEvent(UUID.randomUUID(), ShipmentEventType.SHIPMENT_CREATED,
                UUID.randomUUID(), null, null, "carolina@email.com", null, null,
                ShipmentStatus.CREATED, LocalDateTime.now());

        useCase.process(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo("carolina@email.com");
        assertThat(captor.getValue().getBody()).contains("Tu envío  fue creado");
    }

    private ShipmentEvent validEvent(String email) {
        return new ShipmentEvent(UUID.randomUUID(), ShipmentEventType.SHIPMENT_CREATED, UUID.randomUUID(), "SHP-123",
                "Carolina", email, "Medellín", "Bogotá", ShipmentStatus.CREATED, LocalDateTime.now());
    }

    private ShipmentEvent validEvent(ShipmentEventType eventType) {
        return new ShipmentEvent(UUID.randomUUID(), eventType, UUID.randomUUID(), "SHP-123",
                "Carolina", "carolina@email.com", "Medellín", "Bogotá", ShipmentStatus.CREATED,
                LocalDateTime.now());
    }
}
