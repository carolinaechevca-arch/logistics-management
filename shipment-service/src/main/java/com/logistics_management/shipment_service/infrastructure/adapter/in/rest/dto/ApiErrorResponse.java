package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
}
