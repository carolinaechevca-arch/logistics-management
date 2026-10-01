package com.logistics_management.shipment_service;

import com.logistics_management.shipment_service.application.port.in.CreateShipmentCommand;
import com.logistics_management.shipment_service.application.port.in.CreateShipmentPort;
import com.logistics_management.shipment_service.domain.enums.ShipmentStatus;
import com.logistics_management.shipment_service.domain.model.Shipment;
import com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config.RabbitTopology;
import com.logistics_management.shipment_service.infrastructure.adapter.out.persistence.repository.ShipmentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ShipmentInfrastructureIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("logistics")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    private CreateShipmentPort createShipmentPort;

    @Autowired
    private ShipmentJpaRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void creationPersistsShipmentAndPublishesCreatedEvent() {
        CreateShipmentCommand command = new CreateShipmentCommand(
                "integration-create-1", "Carolina", "carolina@email.com", "Medellín", "Bogotá", "Portátil");
        Shipment created = createShipmentPort.create(command);
        Shipment replayed = createShipmentPort.create(command);

        assertThat(replayed.getId()).isEqualTo(created.getId());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(created.getId())).hasValueSatisfying(entity -> {
            assertThat(entity.getIdempotencyKey()).isEqualTo("integration-create-1");
            assertThat(entity.getStatus()).isEqualTo(ShipmentStatus.CREATED);
            assertThat(entity.getTrackingNumber()).startsWith("SHP-");
        });
        Message received = rabbitTemplate.receive(RabbitTopology.QUEUE, 5000);
        assertThat(received).isNotNull();
        String payload = new String(received.getBody(), StandardCharsets.UTF_8);
        assertThat(payload).contains("SHIPMENT_CREATED", created.getId().toString());
        assertThat(rabbitTemplate.receive(RabbitTopology.QUEUE, 250)).isNull();
    }
}
