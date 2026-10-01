package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record ShipmentPageResponse(
        List<ShipmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
