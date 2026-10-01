package com.logistics_management.notification_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Notification {
    private final String recipient;
    private final String subject;
    private final String body;
}
