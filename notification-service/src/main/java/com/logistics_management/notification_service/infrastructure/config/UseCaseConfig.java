package com.logistics_management.notification_service.infrastructure.config;

import com.logistics_management.notification_service.application.port.in.ProcessShipmentNotificationPort;
import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.application.usecase.ProcessShipmentNotificationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(NotificationMailProperties.class)
public class UseCaseConfig {
    @Bean
    ProcessShipmentNotificationPort processShipmentNotificationUseCase(NotificationSenderPort senderPort) {
        return new ProcessShipmentNotificationUseCase(senderPort);
    }
}
