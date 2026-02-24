package org.example.monitoringservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMeasurementDTO {
    private Long timestamp;
    private Long deviceId;
    private Double measurementValue;
}
