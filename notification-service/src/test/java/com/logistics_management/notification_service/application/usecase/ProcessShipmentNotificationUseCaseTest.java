package com.logistics_management.notification_service.application.usecase;

import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.domain.enums.ShipmentEventType;
import com.logistics_management.notification_service.domain.enums.ShipmentStatus;
import com.logistics_management.notification_service.domain.exception.NotificationProcessingException;
import com.logistics_management.notification_service.domain.model.Notification;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

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

    private ShipmentEvent validEvent(String email) {
        return new ShipmentEvent(UUID.randomUUID(), ShipmentEventType.SHIPMENT_CREATED, UUID.randomUUID(), "SHP-123",
                "Carolina", email, "Medellín", "Bogotá", ShipmentStatus.CREATED, LocalDateTime.now());
    }
}
