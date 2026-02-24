package org.example.websocketmicroservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.websocketmicroservice.dto.NotificationMessage;
import org.example.websocketmicroservice.service.NotificationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @MessageMapping("/notification")
    @SendTo("/topic/notifications")
    public NotificationMessage sendNotification(@Payload NotificationMessage notification) {
        log.info("Received notification via WebSocket: {}", notification);
        
        if (notification.getUserId() != null) {
            notificationService.sendNotificationToUser(notification.getUserId(), notification);
        } else {
            notificationService.broadcastNotification(notification);
        }
        
        return notification;
    }
}