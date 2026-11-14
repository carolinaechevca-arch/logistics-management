package com.logistics_management.notification_service.infrastructure.config;

import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.application.usecase.ProcessShipmentNotificationUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UseCaseConfigTest {
    @Test
    void createsProcessNotificationUseCase() {
        NotificationSenderPort senderPort = mock(NotificationSenderPort.class);

        assertThat(new UseCaseConfig().processShipmentNotificationUseCase(senderPort))
                .isInstanceOf(ProcessShipmentNotificationUseCase.class);
    }

    @Test
    void exposesConfiguredSenderAddress() {
        NotificationMailProperties properties = new NotificationMailProperties("noreply@logistics.com");

        assertThat(properties.getFrom()).isEqualTo("noreply@logistics.com");
    }
}
