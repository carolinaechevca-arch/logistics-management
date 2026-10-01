package com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.publisher;

import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.domain.model.ShipmentEvent;
import com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitShipmentEventPublisher implements ShipmentEventPublisherPort {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(ShipmentEvent event) {
        rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY, event);
        log.info("event published eventId={} shipmentId={} trackingNumber={} eventType={}",
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber(), event.getEventType());
    }
}
