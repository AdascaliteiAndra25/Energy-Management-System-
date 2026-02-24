package org.example.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dtos.DeviceSyncDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncPublisherService {

    private final RabbitTemplate rabbitTemplate;
    
    @Value("${sync.queue.name}")
    private String syncQueueName;

    public void publishDeviceCreated(Long deviceId, String deviceName, Long userId) {
        DeviceSyncDTO syncMessage = new DeviceSyncDTO(
            "DEVICE_CREATED",
            deviceId,
            deviceName,
            userId
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published DEVICE_CREATED event for device: {} (ID: {}) assigned to user: {}", 
            deviceName, deviceId, userId);
    }

    public void publishDeviceUpdated(Long deviceId, String deviceName, Long userId) {
        DeviceSyncDTO syncMessage = new DeviceSyncDTO(
            "DEVICE_UPDATED",
            deviceId,
            deviceName,
            userId
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published DEVICE_UPDATED event for device: {} (ID: {})", deviceName, deviceId);
    }

    public void publishDeviceDeleted(Long deviceId) {
        DeviceSyncDTO syncMessage = new DeviceSyncDTO(
            "DEVICE_DELETED",
            deviceId,
            null,
            null
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published DEVICE_DELETED event for device ID: {}", deviceId);
    }

    public void publishDeviceUserMapping(Long deviceId, Long userId) {
        DeviceSyncDTO syncMessage = new DeviceSyncDTO(
            "DEVICE_USER_MAPPING",
            deviceId,
            null,
            userId
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published DEVICE_USER_MAPPING event: device {} -> user {}", deviceId, userId);
    }
}
