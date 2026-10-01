package com.logistics_management.notification_service;

import com.logistics_management.notification_service.domain.enums.ShipmentEventType;
import com.logistics_management.notification_service.domain.enums.ShipmentStatus;
import com.logistics_management.notification_service.domain.model.ShipmentEvent;
import com.logistics_management.notification_service.infrastructure.config.RabbitTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class NotificationRabbitIntegrationTest {
    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void failedNotificationIsRejectedToDeadLetterQueueAfterRetries() {
        ShipmentEvent event = new ShipmentEvent(UUID.randomUUID(), ShipmentEventType.SHIPMENT_CREATED,
                UUID.randomUUID(), "SHP-FAIL", "Falla", "fail@email.com", "Medellín", "Bogotá",
                ShipmentStatus.CREATED, LocalDateTime.now());

        rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY, event);

        Message deadLetter = rabbitTemplate.receive(RabbitTopology.DEAD_LETTER_QUEUE, 10000);
        assertThat(deadLetter).isNotNull();
    }
}
