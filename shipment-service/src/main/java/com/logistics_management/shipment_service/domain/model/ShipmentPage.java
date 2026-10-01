package com.logistics_management.shipment_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ShipmentPage {
    private final List<Shipment> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
