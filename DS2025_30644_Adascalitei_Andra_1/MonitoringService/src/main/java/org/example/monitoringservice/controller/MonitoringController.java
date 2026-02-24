package org.example.monitoringservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.monitoringservice.entity.HourlyConsumption;
import org.example.monitoringservice.entity.User;
import org.example.monitoringservice.repository.HourlyConsumptionRepository;
import org.example.monitoringservice.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonitoringController {

    private final HourlyConsumptionRepository hourlyConsumptionRepository;
    private final UserRepository userRepository;

    @GetMapping("/user/{userId}/consumption")
    public ResponseEntity<?> getUserConsumption(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<HourlyConsumption> consumptions = hourlyConsumptionRepository
            .findByDeviceIdOrderByTimestampDesc(user.getDeviceId());
        
        return ResponseEntity.ok(consumptions);
    }

    @GetMapping("/user/{userId}/consumption/date")
    public ResponseEntity<?> getUserConsumptionByDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<HourlyConsumption> consumptions = hourlyConsumptionRepository
            .findByDeviceIdAndTimestampBetween(user.getDeviceId(), startOfDay, endOfDay);
        
        return ResponseEntity.ok(consumptions);
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<HourlyConsumption>> getDeviceConsumption(@PathVariable Long deviceId) {
        List<HourlyConsumption> consumptions = hourlyConsumptionRepository
            .findByDeviceIdOrderByTimestampDesc(deviceId);
        return ResponseEntity.ok(consumptions);
    }

    @GetMapping("/device/{deviceId}/date")
    public ResponseEntity<List<HourlyConsumption>> getDeviceConsumptionByDate(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        System.out.println("=== Device Consumption Query ===");
        System.out.println("Device ID: " + deviceId);
        System.out.println("Date: " + date);
        System.out.println("Start: " + startOfDay);
        System.out.println("End: " + endOfDay);
        
        List<HourlyConsumption> consumptions = hourlyConsumptionRepository
            .findByDeviceIdAndTimestampBetween(deviceId, startOfDay, endOfDay);
        
        System.out.println("Found " + consumptions.size() + " records");
        if (!consumptions.isEmpty()) {
            System.out.println("First record: " + consumptions.get(0));
        }
        
        return ResponseEntity.ok(consumptions);
    }

    @GetMapping("/all")
    public ResponseEntity<List<HourlyConsumption>> getAllConsumptions() {
        return ResponseEntity.ok(hourlyConsumptionRepository.findAll());
    }
}