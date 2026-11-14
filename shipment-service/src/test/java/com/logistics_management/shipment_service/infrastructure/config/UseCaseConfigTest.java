package com.logistics_management.shipment_service.infrastructure.config;

import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.application.usecase.CancelShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.CreateShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.DeliverShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.DispatchShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.GetShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.ListShipmentsUseCase;
import com.logistics_management.shipment_service.application.usecase.MarkShipmentInTransitUseCase;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UseCaseConfigTest {
    private final UseCaseConfig config = new UseCaseConfig();

    @Test
    void createsClockAndAllUseCaseBeans() {
        ShipmentPersistencePort persistence = mock(ShipmentPersistencePort.class);
        ShipmentEventPublisherPort publisher = mock(ShipmentEventPublisherPort.class);
        Clock clock = config.clock();

        assertThat(clock.getZone()).isEqualTo(Clock.systemUTC().getZone());
        assertThat(config.createShipmentUseCase(persistence, publisher, clock)).isInstanceOf(CreateShipmentUseCase.class);
        assertThat(config.getShipmentUseCase(persistence)).isInstanceOf(GetShipmentUseCase.class);
        assertThat(config.listShipmentsUseCase(persistence)).isInstanceOf(ListShipmentsUseCase.class);
        assertThat(config.dispatchShipmentUseCase(persistence, publisher, clock)).isInstanceOf(DispatchShipmentUseCase.class);
        assertThat(config.markShipmentInTransitUseCase(persistence, publisher, clock)).isInstanceOf(MarkShipmentInTransitUseCase.class);
        assertThat(config.deliverShipmentUseCase(persistence, publisher, clock)).isInstanceOf(DeliverShipmentUseCase.class);
        assertThat(config.cancelShipmentUseCase(persistence, publisher, clock)).isInstanceOf(CancelShipmentUseCase.class);
    }
}
