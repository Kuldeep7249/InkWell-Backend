package com.inkwell.notification.messaging;

import com.inkwell.notification.dto.SendNotificationRequest;
import com.inkwell.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.notification.queue:notification.queue}")
    public void consume(String payload) {
        try {
            SendNotificationRequest request = objectMapper.readValue(payload, SendNotificationRequest.class);
            notificationService.send(request);
        } catch (JacksonException ex) {
            log.error("Failed to deserialize notification payload: {}", payload, ex);
        } catch (Exception ex) {
            log.error("Failed to process notification payload: {}", payload, ex);
            throw ex;
        }
    }
}
