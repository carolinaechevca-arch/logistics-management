package com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config;

public final class RabbitTopology {
    public static final String EXCHANGE = "shipment.exchange";
    public static final String QUEUE = "shipment.notification.queue";
    public static final String ROUTING_KEY = "shipment.notification";
    public static final String DEAD_LETTER_EXCHANGE = "shipment.dlx";
    public static final String DEAD_LETTER_QUEUE = "shipment.notification.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "shipment.notification.dlq";

    private RabbitTopology() {
    }
}
