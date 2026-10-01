package com.logistics_management.notification_service.application.usecase;

import com.logistics_management.notification_service.application.port.in.ProcessShipmentNotificationPort;
import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.domain.enums.ShipmentEventType;
import com.logistics_management.notification_service.domain.exception.NotificationProcessingException;
import com.logistics_management.notification_service.domain.model.Notification;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class ProcessShipmentNotificationUseCase implements ProcessShipmentNotificationPort {
    private static final String FAILURE_EMAIL = "fail@email.com";
    private final NotificationSenderPort senderPort;
    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    @Override
    public void process(ShipmentEvent event) {
        validate(event);
        log.info("Procesando evento de notificacion. "
                        + "eventId={} shipmentId={} trackingNumber={} eventType={}",
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber(), event.getEventType());
        if (processedEventIds.contains(event.getEventId())) {
            log.info("Idempotencia: evento duplicado detectado. "
                            + "No se enviara otro correo. eventId={} shipmentId={} trackingNumber={}",
                    event.getEventId(), event.getShipmentId(), event.getTrackingNumber());
            return;
        }
        if (FAILURE_EMAIL.equalsIgnoreCase(event.getCustomerEmail())) {
            log.warn("Prueba de fallo: se activo el error controlado para probar reintentos. "
                            + "eventId={} shipmentId={} trackingNumber={}",
                    event.getEventId(), event.getShipmentId(), event.getTrackingNumber());
            throw new NotificationProcessingException("Laboratory failure requested for event " + event.getEventId());
        }
        Notification notification = createNotification(event);
        log.info("Notificacion construida; solicitando envio de correo. "
                        + "eventId={} shipmentId={} asunto={}",
                event.getEventId(), event.getShipmentId(), notification.getSubject());
        senderPort.send(notification);
        processedEventIds.add(event.getEventId());
        log.info("Evento procesado e identificado como completado. "
                        + "eventId={} shipmentId={} trackingNumber={} eventType={}",
                event.getEventId(), event.getShipmentId(), event.getTrackingNumber(), event.getEventType());
    }

    private void validate(ShipmentEvent event) {
        if (event == null || event.getEventId() == null || event.getEventType() == null
                || event.getShipmentId() == null || event.getCustomerEmail() == null
                || event.getCustomerEmail().isBlank()) {
            log.error("Validacion: el evento no tiene todos los campos obligatorios. "
                    + "Se iniciara el mecanismo de reintentos.");
            throw new NotificationProcessingException("Invalid shipment event payload");
        }
    }

    private Notification createNotification(ShipmentEvent event) {
        String subject = switch (event.getEventType()) {
            case SHIPMENT_CREATED -> "Envío creado";
            case SHIPMENT_DISPATCHED -> "Tu envío fue despachado";
            case SHIPMENT_IN_TRANSIT -> "Tu envío está en tránsito";
            case SHIPMENT_DELIVERED -> "Tu envío fue entregado";
            case SHIPMENT_CANCELLED -> "Tu envío fue cancelado";
        };
        String body = event.getEventType() == ShipmentEventType.SHIPMENT_CREATED
                ? "Hola %s,%n%nTu envío %s fue creado.%nOrigen: %s%nDestino: %s"
                    .formatted(value(event.getCustomerName()), value(event.getTrackingNumber()),
                            value(event.getOrigin()), value(event.getDestination()))
                : "Hola %s,%n%nActualización del envío %s: %s."
                    .formatted(value(event.getCustomerName()), value(event.getTrackingNumber()), subject);
        return new Notification(event.getCustomerEmail(), subject, body);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
