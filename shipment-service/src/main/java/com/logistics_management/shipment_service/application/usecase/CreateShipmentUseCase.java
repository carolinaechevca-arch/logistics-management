package com.logistics_management.shipment_service.application.usecase;

import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.domain.enums.ShipmentEventType;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.exception.DuplicateTrackingNumberException;
import com.logistics_management.shipment_service.domain.exception.IdempotencyKeyConflictException;
import com.logistics_management.shipment_service.domain.exception.InvalidIdempotencyKeyException;
import com.logistics_management.shipment_service.domain.model.Shipment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class CreateShipmentUseCase implements CreateShipmentPort {
    private static final int MAX_TRACKING_ATTEMPTS = 10;
    private final ShipmentPersistencePort persistencePort;
    private final ShipmentEventPublisherPort eventPublisherPort;
    private final Clock clock;

    @Override
    public synchronized Shipment create(CreateShipmentCommand command) {
        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        log.info("Validando la clave de idempotencia. idempotencyKey={}",
                idempotencyKey);
        Shipment existing = persistencePort.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!matches(existing, command)) {
                log.warn("Idempotencia: conflicto porque la clave ya existe con datos diferentes. "
                                + "idempotencyKey={} shipmentId={} trackingNumber={}",
                        idempotencyKey, existing.getId(), existing.getTrackingNumber());
                throw new IdempotencyKeyConflictException(idempotencyKey);
            }
            log.info("Idempotencia: solicitud repetida detectada. Se devuelve el envio existente "
                            + "y NO se guarda ni se publica otro evento. idempotencyKey={} shipmentId={} trackingNumber={}",
                    idempotencyKey, existing.getId(), existing.getTrackingNumber());
            return existing;
        }
        log.info("Idempotencia: la clave es nueva; se continuara con la creacion. idempotencyKey={}",
                idempotencyKey);
        LocalDateTime now = LocalDateTime.now(clock);
        String trackingNumber = generateUniqueTrackingNumber();
        Shipment shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .trackingNumber(trackingNumber)
                .customerName(command.customerName())
                .customerEmail(command.customerEmail())
                .origin(command.origin())
                .destination(command.destination())
                .description(command.description())
                .status(ShipmentStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Shipment saved = persistencePort.save(shipment);
        log.info("Envio creado correctamente. "
                        + "shipmentId={} trackingNumber={} status={}",
                saved.getId(), saved.getTrackingNumber(), saved.getStatus());
        log.info("Solicitando la publicacion del evento SHIPMENT_CREATED. "
                        + "shipmentId={} trackingNumber={}", saved.getId(), saved.getTrackingNumber());
        eventPublisherPort.publish(ShipmentEventFactory.create(saved, ShipmentEventType.SHIPMENT_CREATED, now));
        return saved;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new InvalidIdempotencyKeyException();
        }
        return idempotencyKey.trim();
    }

    private boolean matches(Shipment shipment, CreateShipmentCommand command) {
        return Objects.equals(shipment.getCustomerName(), command.customerName())
                && Objects.equals(shipment.getCustomerEmail(), command.customerEmail())
                && Objects.equals(shipment.getOrigin(), command.origin())
                && Objects.equals(shipment.getDestination(), command.destination())
                && Objects.equals(shipment.getDescription(), command.description());
    }

    private String generateUniqueTrackingNumber() {
        String trackingNumber = null;
        for (int attempt = 0; attempt < MAX_TRACKING_ATTEMPTS; attempt++) {
            trackingNumber = "SHP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            if (!persistencePort.existsByTrackingNumber(trackingNumber)) {
                return trackingNumber;
            }
        }
        throw new DuplicateTrackingNumberException(trackingNumber);
    }
}
