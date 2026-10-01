package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.mapper;

import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.domain.model.ShipmentPage;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.CreateShipmentRequest;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentPageResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentResponse;
import org.springframework.stereotype.Component;

@Component
public class ShipmentRestMapper {
    public CreateShipmentCommand toCommand(String idempotencyKey, CreateShipmentRequest request) {
        return new CreateShipmentCommand(idempotencyKey, request.customerName(), request.customerEmail(), request.origin(),
                request.destination(), request.description());
    }

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getCustomerName(),
                shipment.getCustomerEmail(), shipment.getOrigin(), shipment.getDestination(), shipment.getDescription(),
                shipment.getStatus(), shipment.getCreatedAt(), shipment.getUpdatedAt());
    }

    public ShipmentPageResponse toResponse(ShipmentPage page) {
        return new ShipmentPageResponse(page.getContent().stream().map(this::toResponse).toList(),
                page.getPage(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
