package com.logistics_management.notification_service.infrastructure.adapter.in.messaging.consumer;

import com.logistics_management.notification_service.application.port.in.ProcessShipmentNotificationPort;
import com.logistics_management.notification_service.domain.enums.ShipmentEventType;
import com.logistics_management.notification_service.domain.enums.ShipmentStatus;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShipmentEventConsumerTest {
    @Mock ProcessShipmentNotificationPort processPort;
    @InjectMocks ShipmentEventConsumer consumer;

    @Test
    void processesEventAndAcknowledgesSuccessfulAttempt() {
        ShipmentEvent event = event(UUID.randomUUID());

        consumer.receive(event);
        consumer.receive(event);

        verify(processPort, times(2)).process(event);
    }

    @Test
    void propagatesFailuresAndClearsAttemptAfterThirdFailure() {
        ShipmentEvent event = event(UUID.randomUUID());
        IllegalStateException failure = new IllegalStateException("SMTP unavailable");
        doThrow(failure).when(processPort).process(event);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> consumer.receive(event)).isSameAs(failure);
        }

        verify(processPort, times(4)).process(event);
    }

    @Test
    void supportsEventsWithoutIdentifier() {
        ShipmentEvent event = event(null);

        consumer.receive(event);

        verify(processPort).process(event);
    }

    private ShipmentEvent event(UUID eventId) {
        return new ShipmentEvent(eventId, ShipmentEventType.SHIPMENT_CREATED, UUID.randomUUID(), "TRK-1",
                "Carolina", "carolina@example.com", "Bogota", "Medellin", ShipmentStatus.CREATED,
                LocalDateTime.of(2026, 11, 13, 12, 0));
    }
}
