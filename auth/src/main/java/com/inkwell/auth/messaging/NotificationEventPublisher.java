package com.inkwell.auth.messaging;

import com.inkwell.auth.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.notification.exchange:notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.notification.routing-key:notification.created}")
    private String routingKey;

    public void publish(SendNotificationRequest request) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(request));
        } catch (JacksonException ex) {
            log.error("Failed to serialize notification event for recipientId={}", request.getRecipientId(), ex);
        } catch (Exception ex) {
            log.error("Failed to publish notification event for recipientId={}", request.getRecipientId(), ex);
        }
    }
}
