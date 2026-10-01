package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateShipmentRequest(
        @NotBlank String customerName,
        @NotBlank @Email String customerEmail,
        @NotBlank String origin,
        @NotBlank String destination,
        String description) {
}
