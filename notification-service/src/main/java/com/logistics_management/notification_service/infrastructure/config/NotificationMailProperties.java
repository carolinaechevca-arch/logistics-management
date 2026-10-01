package com.logistics_management.notification_service.infrastructure.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "notification.mail")
public class NotificationMailProperties {
    private final String from;
}
