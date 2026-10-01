package com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.entity;

import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments", schema = "shipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentEntity {
    @Id
    private UUID id;
    @Column(unique = true, length = 128)
    private String idempotencyKey;
    @Column(nullable = false, unique = true, length = 64)
    private String trackingNumber;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String customerEmail;
    @Column(nullable = false)
    private String origin;
    @Column(nullable = false)
    private String destination;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
