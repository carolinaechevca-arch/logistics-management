package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateShipmentRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio") String customerName,
        @NotBlank(message = "El correo del cliente es obligatorio")
        @Email(message = "El correo del cliente debe tener un formato valido") String customerEmail,
        @NotBlank(message = "El origen del envio es obligatorio") String origin,
        @NotBlank(message = "El destino del envio es obligatorio") String destination,
        String description) {
}
