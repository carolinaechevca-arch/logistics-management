package com.logistics_management.notification_service.infrastructure.config;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RabbitConfigTest {
    private final RabbitConfig config = new RabbitConfig();

    @Test
    void createsRabbitTopologyAndJsonConverter() {
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

    @Test
    void createsRecovererRetryAdviceAndListenerFactory() {
        var recoverer = config.notificationMessageRecoverer();
        Advice advice = config.notificationRetryAdvice(recoverer);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter converter = config.rabbitMessageConverter();

        assertThat(config.rabbitListenerContainerFactory(connectionFactory, converter, advice)).isNotNull();

        Message message = new Message(new byte[0]);
        message.getMessageProperties().setReceivedExchange("shipment.exchange");
        message.getMessageProperties().setReceivedRoutingKey("shipment.notification");
        message.getMessageProperties().setConsumerQueue("shipment.notification.queue");
        IllegalStateException failure = new IllegalStateException("processing failed");
        assertThatThrownBy(() -> recoverer.recover(message, failure))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Retry Policy Exhausted");
    }
}
