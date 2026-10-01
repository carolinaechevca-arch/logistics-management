package com.logistics_management.notification_service.application.port.in;

import com.logistics_management.notification_service.domain.model.ShipmentEvent;

public interface ProcessShipmentNotificationPort {
    void process(ShipmentEvent event);
}
