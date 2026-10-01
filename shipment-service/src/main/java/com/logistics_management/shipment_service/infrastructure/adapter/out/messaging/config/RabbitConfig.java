package com.logistics_management.shipment_service.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange shipmentExchange() {
        return new TopicExchange(RabbitTopology.EXCHANGE, true, false);
    }

    @Bean
    TopicExchange shipmentDeadLetterExchange() {
        return new TopicExchange(RabbitTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue shipmentNotificationQueue() {
        return QueueBuilder.durable(RabbitTopology.QUEUE)
                .deadLetterExchange(RabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue shipmentNotificationDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding shipmentNotificationBinding(Queue shipmentNotificationQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(shipmentNotificationQueue).to(shipmentExchange).with(RabbitTopology.ROUTING_KEY);
    }

    @Bean
    Binding shipmentNotificationDeadLetterBinding(Queue shipmentNotificationDeadLetterQueue,
                                                   TopicExchange shipmentDeadLetterExchange) {
        return BindingBuilder.bind(shipmentNotificationDeadLetterQueue)
                .to(shipmentDeadLetterExchange)
                .with(RabbitTopology.DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
