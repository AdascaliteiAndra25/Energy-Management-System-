package org.example.monitoringservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    private static final String NOTIFICATION_EXCHANGE = "notification_exchange";
    private static final String OVERCONSUMPTION_ROUTING_KEY = "notification.overconsumption";

    public void sendOverconsumptionNotification(Long deviceId, Long userId, Double consumption, Double threshold) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("deviceId", deviceId);
        notification.put("deviceName", "Device " + deviceId);
        notification.put("consumption", consumption);
        notification.put("maxConsumption", threshold);
        notification.put("timestamp", System.currentTimeMillis());

        double overPercentage = ((consumption - threshold) / threshold) * 100;
        notification.put("overPercentage", overPercentage);

        try {
            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, OVERCONSUMPTION_ROUTING_KEY, notification);
            log.info("Sent overconsumption notification for device {} to user {}: {} kWh > {} kWh ({}% over)",
                deviceId, userId, consumption, threshold, String.format("%.1f", overPercentage));
        } catch (Exception e) {
            log.error("Failed to send overconsumption notification: {}", e.getMessage(), e);
        }
    }
}