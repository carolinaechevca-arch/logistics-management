package com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {
    private final RabbitConfig config = new RabbitConfig();

    @Test
    void createsExchangesQueuesBindingsAndJsonConverter() {
        var exchange = config.shipmentExchange();
        var deadLetterExchange = config.shipmentDeadLetterExchange();
        var queue = config.shipmentNotificationQueue();
        var deadLetterQueue = config.shipmentNotificationDeadLetterQueue();
        var binding = config.shipmentNotificationBinding(queue, exchange);
        var deadLetterBinding = config.shipmentNotificationDeadLetterBinding(deadLetterQueue, deadLetterExchange);

        assertThat(exchange.getName()).isEqualTo(RabbitTopology.EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(deadLetterExchange.getName()).isEqualTo(RabbitTopology.DEAD_LETTER_EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitTopology.QUEUE);
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", RabbitTopology.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitTopology.DEAD_LETTER_ROUTING_KEY);
        assertThat(deadLetterQueue.getName()).isEqualTo(RabbitTopology.DEAD_LETTER_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitTopology.EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitTopology.ROUTING_KEY);
        assertThat(deadLetterBinding.getExchange()).isEqualTo(RabbitTopology.DEAD_LETTER_EXCHANGE);
        assertThat(deadLetterBinding.getRoutingKey()).isEqualTo(RabbitTopology.DEAD_LETTER_ROUTING_KEY);
        assertThat(config.rabbitMessageConverter()).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
