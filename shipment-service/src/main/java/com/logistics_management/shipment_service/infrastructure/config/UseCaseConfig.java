package com.logistics_management.shipment_service.infrastructure.config;

import com.logistics_management.shipment_service.application.port.in.CancelShipmentPort;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DeliverShipmentPort;
import com.logistics_management.shipment_service.application.port.in.DispatchShipmentPort;
import com.logistics_management.shipment_service.application.port.in.GetShipmentPort;
import com.logistics_management.shipment_service.application.port.in.ListShipmentsPort;
import com.logistics_management.shipment_service.application.port.in.MarkShipmentInTransitPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentEventPublisherPort;
import com.logistics_management.shipment_service.application.port.out.ShipmentPersistencePort;
import com.logistics_management.shipment_service.application.usecase.CancelShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.CreateShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.DeliverShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.DispatchShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.GetShipmentUseCase;
import com.logistics_management.shipment_service.application.usecase.ListShipmentsUseCase;
import com.logistics_management.shipment_service.application.usecase.MarkShipmentInTransitUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateShipmentPort createShipmentUseCase(ShipmentPersistencePort persistence, ShipmentEventPublisherPort publisher, Clock clock) {
        return new CreateShipmentUseCase(persistence, publisher, clock);
    }

    @Bean
    GetShipmentPort getShipmentUseCase(ShipmentPersistencePort persistence) {
        return new GetShipmentUseCase(persistence);
    }

    @Bean
    ListShipmentsPort listShipmentsUseCase(ShipmentPersistencePort persistence) {
        return new ListShipmentsUseCase(persistence);
    }

    @Bean
    DispatchShipmentPort dispatchShipmentUseCase(ShipmentPersistencePort persistence, ShipmentEventPublisherPort publisher, Clock clock) {
        return new DispatchShipmentUseCase(persistence, publisher, clock);
    }

    @Bean
    MarkShipmentInTransitPort markShipmentInTransitUseCase(ShipmentPersistencePort persistence, ShipmentEventPublisherPort publisher, Clock clock) {
        return new MarkShipmentInTransitUseCase(persistence, publisher, clock);
    }

    @Bean
    DeliverShipmentPort deliverShipmentUseCase(ShipmentPersistencePort persistence, ShipmentEventPublisherPort publisher, Clock clock) {
        return new DeliverShipmentUseCase(persistence, publisher, clock);
    }

    @Bean
    CancelShipmentPort cancelShipmentUseCase(ShipmentPersistencePort persistence, ShipmentEventPublisherPort publisher, Clock clock) {
        return new CancelShipmentUseCase(persistence, publisher, clock);
    }
}
