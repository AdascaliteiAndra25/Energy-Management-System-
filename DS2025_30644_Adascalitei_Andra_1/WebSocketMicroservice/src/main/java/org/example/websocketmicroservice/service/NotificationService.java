package org.example.websocketmicroservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.websocketmicroservice.dto.NotificationMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(Long userId, NotificationMessage notification) {
        try {
            log.info("Sending notification to user {}: {}", userId, notification.getMessage());
            
            // Send to specific user's notification topic
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
            
            // Also send to general notifications topic for admin monitoring
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            
            log.info("Notification sent successfully to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    public void sendNotificationToAdmins(NotificationMessage notification) {
        try {
            log.info("Sending notification to admins: {}", notification.getMessage());
            
            // Send to admin notification topic
            messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
            
            log.info("Admin notification sent successfully");
        } catch (Exception e) {
            log.error(" Failed to send admin notification: {}", e.getMessage(), e);
        }
    }

    public void broadcastNotification(NotificationMessage notification) {
        try {
            log.info("Broadcasting notification: {}", notification.getMessage());
            
            // Broadcast to all users
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            
            log.info("Notification broadcasted successfully");
        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage(), e);
        }
    }
}