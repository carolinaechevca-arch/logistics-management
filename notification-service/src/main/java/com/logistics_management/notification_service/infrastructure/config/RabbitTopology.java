package com.logistics_management.notification_service.infrastructure.config;

public final class RabbitTopology {
    public static final String EXCHANGE = "shipment.exchange";
    public static final String QUEUE = "shipment.notification.queue";
    public static final String ROUTING_KEY = "shipment.notification";
    public static final String DEAD_LETTER_EXCHANGE = "shipment.dlx";
    public static final String DEAD_LETTER_QUEUE = "shipment.notification.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "shipment.notification.dlq";
    public static final int MAX_PROCESSING_ATTEMPTS = 3;

    private RabbitTopology() {
    }
}
