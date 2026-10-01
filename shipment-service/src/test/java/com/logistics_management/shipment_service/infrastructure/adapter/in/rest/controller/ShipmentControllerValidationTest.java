package com.logistics_management.shipment_service.infrastructure.adapter.in.rest.controller;

import com.logistics_management.shipment_service.application.port.in.CancelShipmentPort;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DeliverShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DispatchShipmentPort;
import com.logistics_management.shipment_service.application.port.in.GetShipmentPort;
import com.logistics_management.shipment_service.application.port.in.ListShipmentsPort;
import com.logistics_management.shipment_service.application.port.in.MarkShipmentInTransitPort;
import com.logistics_management.shipment_service.infrastructure.adapter.in.rest.mapper.ShipmentRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShipmentControllerValidationTest {
    @Test
    void invalidEmailReturnsBadRequestWithoutCallingUseCase() throws Exception {
        CreateShipmentPort createPort = mock(CreateShipmentPort.class);
        ShipmentController controller = new ShipmentController(createPort, mock(GetShipmentPort.class),
                mock(ListShipmentsPort.class), mock(DispatchShipmentPort.class),
                mock(MarkShipmentInTransitPort.class), mock(DeliverShipmentPort.class),
                mock(CancelShipmentPort.class), new ShipmentRestMapper());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mockMvc.perform(post("/api/v1/shipments")
                        .header("Idempotency-Key", "validation-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Carolina","customerEmail":"invalid",
                                "origin":"Medellín","destination":"Bogotá"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("customerEmail: El correo del cliente debe tener un formato valido"));

        verifyNoInteractions(createPort);
    }
}
