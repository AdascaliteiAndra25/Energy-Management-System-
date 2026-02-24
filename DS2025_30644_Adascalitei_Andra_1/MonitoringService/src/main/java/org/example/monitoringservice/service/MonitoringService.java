package org.example.monitoringservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.monitoringservice.dto.DeviceMeasurementDTO;
import org.example.monitoringservice.entity.HourlyConsumption;
import org.example.monitoringservice.entity.User;
import org.example.monitoringservice.repository.HourlyConsumptionRepository;
import org.example.monitoringservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final HourlyConsumptionRepository hourlyConsumptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    private final Map<String, List<DeviceMeasurementDTO>> hourlyBuffer = new ConcurrentHashMap<>();

    public void processMeasurement(DeviceMeasurementDTO measurement) {
        log.info("Processing measurement: {}", measurement);
        
        LocalDateTime measurementTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(measurement.getTimestamp()), 
            ZoneId.systemDefault()
        );
        
        LocalDateTime hourStart = measurementTime.truncatedTo(ChronoUnit.HOURS);
        String bufferKey = measurement.getDeviceId() + "_" + hourStart;
        
        hourlyBuffer.computeIfAbsent(bufferKey, k -> new ArrayList<>()).add(measurement);
        
        List<DeviceMeasurementDTO> measurements = hourlyBuffer.get(bufferKey);
        
        log.info("Buffer for device {} at {}: {}/6 measurements", 
            measurement.getDeviceId(), hourStart, measurements.size());
        
        if (measurements.size() >= 6) {
            double totalConsumption = measurements.stream()
                .mapToDouble(DeviceMeasurementDTO::getMeasurementValue)
                .sum();
            
            HourlyConsumption hourlyConsumption = new HourlyConsumption();
            hourlyConsumption.setDeviceId(measurement.getDeviceId());
            hourlyConsumption.setTimestamp(hourStart);
            hourlyConsumption.setEnergyConsumption(totalConsumption);
            
            hourlyConsumptionRepository.save(hourlyConsumption);
            log.info("✅Hourly consumption saved: {} kWh for device {} at {}",
                totalConsumption, measurement.getDeviceId(), hourStart);
            checkOverconsumption(measurement.getDeviceId(), totalConsumption);
            
            hourlyBuffer.remove(bufferKey);
        }
    }

    private void checkOverconsumption(Long deviceId, Double totalConsumption) {
        try {
            if (deviceId != 2L) {
                log.debug("Overconsumption check skipped for device {} (only checking device 2)", deviceId);
                return;
            }
            
            Double maxConsumption = 5.0;

            Long userId = getUserIdForDevice(deviceId);
            if (userId == null) {
                log.warn("NO user found for device {}, skipping overconsumption check", deviceId);
                return;
            }
            
            log.info("Checking overconsumption for device {}: Total={} kWh, Max={} kWh",
                deviceId, totalConsumption, maxConsumption);
            
            if (totalConsumption > maxConsumption) {
                log.warn("!OVERCONSUMPTION ALERT! Device {} - Total: {} kWh, Max: {} kWh",
                    deviceId, totalConsumption, maxConsumption);
                
                try {
                    notificationService.sendOverconsumptionNotification(
                        deviceId, 
                        userId,
                        totalConsumption, 
                        maxConsumption
                    );
                    
                    log.info("Overconsumption notification sent for device {} to user {}", deviceId, userId);
                } catch (Exception e) {
                    log.error("Failed to send notification: {}", e.getMessage(), e);
                }
            } else {
                log.info("✅Device {} within limits: {}/{} kWh",
                    deviceId, totalConsumption, maxConsumption);
            }
        } catch (Exception e) {
            log.error("❌Error checking overconsumption for device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    private Long getUserIdForDevice(Long deviceId) {
        try {

            List<User> users = userRepository.findAll();
            for (User user : users) {
                if (deviceId.equals(user.getDeviceId())) {
                    log.debug("Found user {} for device {}", user.getId(), deviceId);
                    return user.getId();
                }
            }
            log.warn("No user found for device {}", deviceId);
            return null;
        } catch (Exception e) {
            log.error("Error finding user for device {}: {}", deviceId, e.getMessage());
            return null;
        }
    }
}