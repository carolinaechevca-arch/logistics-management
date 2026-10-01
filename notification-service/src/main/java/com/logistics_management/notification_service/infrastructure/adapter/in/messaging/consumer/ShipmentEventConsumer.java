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
        log.info("Consumidor RabbitMQ: evento recibido. cola={} "
                        + "evento={{eventId={}, eventType={}, shipmentId={}, trackingNumber={}, status={}, "
                        + "origin={}, destination={}, occurredAt={}}}",
                RabbitTopology.QUEUE, event.getEventId(), event.getEventType(), event.getShipmentId(),
                event.getTrackingNumber(), event.getStatus(), event.getOrigin(), event.getDestination(),
                event.getOccurredAt());
        log.info("Reintento: iniciando intento {}/{}. "
                        + "eventId={} shipmentId={} trackingNumber={}",
                attempt, RabbitTopology.MAX_PROCESSING_ATTEMPTS,
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber());
        try {
            processPort.process(event);
            log.info("Consumidor RabbitMQ: intento {}/{} completado. "
                            + "RabbitMQ confirmara el mensaje (ACK). eventId={} shipmentId={}",
                    attempt, RabbitTopology.MAX_PROCESSING_ATTEMPTS, event.getEventId(), event.getShipmentId());
            processingAttempts.remove(attemptKey);
        } catch (RuntimeException exception) {
            log.error("Reintento: fallo el intento {}/{}. "
                            + "eventId={} shipmentId={} trackingNumber={} causa={} mensaje={}",
                    attempt, RabbitTopology.MAX_PROCESSING_ATTEMPTS,
                    event.getEventId(), event.getShipmentId(), event.getTrackingNumber(),
                    exception.getClass().getSimpleName(), exception.getMessage());
            if (attempt >= RabbitTopology.MAX_PROCESSING_ATTEMPTS) {
                log.error("DLQ: se agotaron los {} intentos. "
                                + "El mensaje sera rechazado sin reencolar y enviado a la DLQ. eventId={} shipmentId={}",
                        RabbitTopology.MAX_PROCESSING_ATTEMPTS, event.getEventId(), event.getShipmentId());
                processingAttempts.remove(attemptKey);
            }
            throw exception;
        }
    }
}
