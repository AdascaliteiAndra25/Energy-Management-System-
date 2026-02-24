package org.example.websocketmicroservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.example.websocketmicroservice.config.RabbitMQConfig;
import org.example.websocketmicroservice.dto.NotificationMessage;
import org.example.websocketmicroservice.service.NotificationService;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverconsumptionListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.OVERCONSUMPTION_QUEUE)
    public void handleOverconsumptionAlert(Map<String, Object> message) {
        try {
            log.info("Received overconsumption alert: {}", message);

            // Extract data from message
            Long userId = getLongValue(message.get("userId"));
            Long deviceId = getLongValue(message.get("deviceId"));
            String deviceName = (String) message.get("deviceName");
            Double consumption = getDoubleValue(message.get("consumption"));
            Double maxConsumption = getDoubleValue(message.get("maxConsumption"));
            Double overPercentage = getDoubleValue(message.get("overPercentage"));

            // Validate required fields
            if (userId == null || deviceId == null) {
                log.error("Invalid overconsumption message: missing userId or deviceId");
                return;
            }

            // Calculate percentage if not provided
            if (overPercentage == null && consumption != null && maxConsumption != null) {
                overPercentage = ((consumption - maxConsumption) / maxConsumption) * 100;
            }

            // Determine severity
            String severity = determineSeverity(overPercentage != null ? overPercentage : 0);

            // Create notification message
            String notificationText = String.format(
                "⚠️ Device '%s' exceeded maximum consumption! Current: %.2f kWh, Max: %.2f kWh (%.1f%% over limit)",
                deviceName != null ? deviceName : "Device " + deviceId,
                consumption != null ? consumption : 0.0,
                maxConsumption != null ? maxConsumption : 0.0,
                overPercentage != null ? overPercentage : 0.0
            );

            NotificationMessage notification = NotificationMessage.builder()
                    .notificationId(System.currentTimeMillis())
                    .userId(userId)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .type(NotificationMessage.NotificationType.OVERCONSUMPTION)
                    .message(notificationText)
                    .currentConsumption(consumption)
                    .maxConsumption(maxConsumption)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .severity(severity)
                    .build();

            // Send notification to specific user
            notificationService.sendNotificationToUser(userId, notification);
            log.info("Notification sent to user {}", userId);

            log.info("Successfully processed overconsumption alert for device {} (User: {})",
                deviceId, userId);

        } catch (Exception e) {
            log.error("Error processing overconsumption alert: {}", e.getMessage(), e);
        }
    }

    private String determineSeverity(double overPercentage) {
        if (overPercentage > 50) {
            return "CRITICAL";
        } else if (overPercentage > 20) {
            return "WARNING";
        } else {
            return "INFO";
        }
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse Long from string: {}", value);
                return null;
            }
        }
        return null;
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse Double from string: {}", value);
                return null;
            }
        }
        return null;
    }
}