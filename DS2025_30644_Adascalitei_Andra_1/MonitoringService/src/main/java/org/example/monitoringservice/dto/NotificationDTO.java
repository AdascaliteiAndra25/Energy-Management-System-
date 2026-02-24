package org.example.monitoringservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String type;
    private Long userId;
    private Long deviceId;
    private String message;
    private Double consumptionValue;
    private Double threshold;
    private Long timestamp;
}