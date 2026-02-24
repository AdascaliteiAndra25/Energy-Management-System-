package org.example.websocketmicroservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long notificationId;
    private Long userId;
    private Long deviceId;
    private String deviceName;
    private NotificationType type;
    private String message;
    private Double currentConsumption;
    private Double maxConsumption;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private boolean read;
    private String severity; // INFO, WARNING, CRITICAL

    public enum NotificationType {
        OVERCONSUMPTION,
        DEVICE_OFFLINE,
        DEVICE_ONLINE,
        SYSTEM_ALERT,
        CHAT_MESSAGE
    }
}