package com.inkwell.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue notificationQueue(@Value("${app.rabbitmq.notification.queue:notification.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange notificationExchange(@Value("${app.rabbitmq.notification.exchange:notification.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding notificationBinding(
            Queue notificationQueue,
            TopicExchange notificationExchange,
            @Value("${app.rabbitmq.notification.routing-key:notification.created}") String routingKey
    ) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(routingKey);
    }
}
