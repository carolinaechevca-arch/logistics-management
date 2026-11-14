package com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.publisher;

import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.ShipmentEvent;
import com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config.RabbitTopology;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitShipmentEventPublisherTest {
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks RabbitShipmentEventPublisher publisher;

    @Test
    void publishesEventToConfiguredExchangeAndRoutingKey() {
        ShipmentEvent event = event();

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY, event);
    }

    @Test
    void propagatesRabbitFailure() {
        ShipmentEvent event = event();
        IllegalStateException failure = new IllegalStateException("Rabbit unavailable");
        doThrow(failure).when(rabbitTemplate)
                .convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY, event);

        assertThatThrownBy(() -> publisher.publish(event)).isSameAs(failure);
    }

    private ShipmentEvent event() {
        return ShipmentEvent.builder().eventId(UUID.randomUUID()).eventType(ShipmentEventType.SHIPMENT_CREATED)
                .shipmentId(UUID.randomUUID()).trackingNumber("TRK-1").customerName("Carolina")
                .customerEmail("carolina@example.com").origin("Bogota").destination("Cali")
                .status(ShipmentStatus.CREATED).occurredAt(LocalDateTime.now()).build();
    }
}
