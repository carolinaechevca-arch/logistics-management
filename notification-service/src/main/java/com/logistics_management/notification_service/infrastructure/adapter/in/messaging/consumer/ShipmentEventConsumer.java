package com.logistics_management.notification_service.infrastructure.adapter.in.messaging.consumer;

import com.logistics_management.notification_service.application.port.in.ProcessShipmentNotificationPort;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import com.logistics_management.notification_service.infrastructure.config.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentEventConsumer {
    private final ProcessShipmentNotificationPort processPort;
    private final Map<String, Integer> processingAttempts = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitTopology.QUEUE)
    public void receive(ShipmentEvent event) {
        String attemptKey = event.getEventId() == null
                ? "invalid-" + System.identityHashCode(event)
                : event.getEventId().toString();
        int attempt = processingAttempts.merge(attemptKey, 1, Integer::sum);
        log.info("event received eventId={} shipmentId={} trackingNumber={} eventType={}",
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber(), event.getEventType());
        log.info("notification processing attempt {}/{} eventId={} shipmentId={} trackingNumber={}",
                attempt, RabbitTopology.MAX_PROCESSING_ATTEMPTS,
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber());
        try {
            processPort.process(event);
            processingAttempts.remove(attemptKey);
        } catch (RuntimeException exception) {
            log.error("notification processing attempt {}/{} failed eventId={} shipmentId={} trackingNumber={}",
                    attempt, RabbitTopology.MAX_PROCESSING_ATTEMPTS,
                    event.getEventId(), event.getShipmentId(), event.getTrackingNumber(), exception);
            if (attempt >= RabbitTopology.MAX_PROCESSING_ATTEMPTS) {
                processingAttempts.remove(attemptKey);
            }
            throw exception;
        }
    }
}
