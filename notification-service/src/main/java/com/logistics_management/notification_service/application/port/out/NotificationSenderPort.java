package com.logistics_management.notification_service.application.port.out;

import com.logistics_management.notification_service.domain.model.Notification;

public interface NotificationSenderPort {
    void send(Notification notification);
}
