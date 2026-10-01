package com.logistics_management.notification_service.infrastructure.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
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

    @Bean
    MessageRecoverer notificationMessageRecoverer() {
        RejectAndDontRequeueRecoverer delegate = new RejectAndDontRequeueRecoverer();
        return (message, cause) -> {
            log.error("DLQ: RabbitMQ rechazo definitivamente el mensaje. "
                            + "exchange={} routingKey={} consumerQueue={} causa={} mensaje={}",
                    message.getMessageProperties().getReceivedExchange(),
                    message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getConsumerQueue(),
                    cause.getClass().getSimpleName(), cause.getMessage());
            delegate.recover(message, cause);
        };
    }

    @Bean
    Advice notificationRetryAdvice(MessageRecoverer notificationMessageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(RabbitTopology.MAX_PROCESSING_ATTEMPTS - 1)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer(notificationMessageRecoverer)
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                        MessageConverter rabbitMessageConverter,
                                                                        Advice notificationRetryAdvice) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(notificationRetryAdvice);
        return factory;
    }
}
