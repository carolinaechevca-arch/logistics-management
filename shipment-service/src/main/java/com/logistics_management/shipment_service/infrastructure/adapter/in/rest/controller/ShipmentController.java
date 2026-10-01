package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.controller;

import com.logistics_management.shipment_service.application.port.in.CancelShipmentPort;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DeliverShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DispatchShipmentPort;
import com.logistics_management.shipment_service.application.port.in.GetShipmentPort;
import com.logistics_management.shipment_service.application.port.in.ListShipmentsPort;
import com.logistics_management.shipment_service.application.port.in.MarkShipmentInTransitPort;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ApiErrorResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.CreateShipmentRequest;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentPageResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.dto.ShipmentResponse;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.mapper.ShipmentRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final CreateShipmentPort createShipmentPort;
    private final GetShipmentPort getShipmentPort;
    private final ListShipmentsPort listShipmentsPort;
    private final DispatchShipmentPort dispatchShipmentPort;
    private final MarkShipmentInTransitPort markShipmentInTransitPort;
    private final DeliverShipmentPort deliverShipmentPort;
    private final CancelShipmentPort cancelShipmentPort;
    private final ShipmentRestMapper mapper;

    @PostMapping
    @Operation(summary = "Create a shipment")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Shipment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency key reused with a different request")})
    public ResponseEntity<ShipmentResponse> create(
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(max = 128)
            @Parameter(description = "Unique key for safely retrying this creation request", required = true)
            String idempotencyKey,
            @Valid @RequestBody CreateShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(createShipmentPort.create(mapper.toCommand(idempotencyKey, request))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a shipment by id")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Shipment found"), @ApiResponse(responseCode = "404", description = "Shipment not found")})
    public ShipmentResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getShipmentPort.getById(id));
    }

    @GetMapping
    @Operation(summary = "List shipments with optional status filtering")
    public ShipmentPageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
                                     @RequestParam(required = false) ShipmentStatus status) {
        return mapper.toResponse(listShipmentsPort.list(page, size, status));
    }

    @PatchMapping("/{id}/dispatch")
    @Operation(summary = "Dispatch a created shipment")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Shipment dispatched"), @ApiResponse(responseCode = "404", description = "Shipment not found"), @ApiResponse(responseCode = "409", description = "Invalid transition")})
    public ShipmentResponse dispatch(@PathVariable UUID id) {
        return mapper.toResponse(dispatchShipmentPort.dispatch(id));
    }

    @PatchMapping("/{id}/in-transit")
    @Operation(summary = "Mark a dispatched shipment as in transit")
    public ShipmentResponse markInTransit(@PathVariable UUID id) {
        return mapper.toResponse(markShipmentInTransitPort.markInTransit(id));
    }

    @PatchMapping("/{id}/deliver")
    @Operation(summary = "Deliver a shipment in transit")
    public ShipmentResponse deliver(@PathVariable UUID id) {
        return mapper.toResponse(deliverShipmentPort.deliver(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Logically cancel a created shipment")
    public ShipmentResponse cancel(@PathVariable UUID id) {
        return mapper.toResponse(cancelShipmentPort.cancel(id));
    }
}
