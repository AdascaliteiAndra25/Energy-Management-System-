package org.example.UserMicroservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.UserMicroservice.dtos.UserSyncDTO;
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

    public void publishUserCreated(Long userId, String username) {
        UserSyncDTO syncMessage = new UserSyncDTO(
            "USER_CREATED",
            userId,
            username,
            null
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published USER_CREATED event for user: {} (ID: {})", username, userId);
    }

    public void publishUserUpdated(Long userId, String username) {
        UserSyncDTO syncMessage = new UserSyncDTO(
            "USER_UPDATED",
            userId,
            username,
            null
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published USER_UPDATED event for user: {} (ID: {})", username, userId);
    }

    public void publishUserDeleted(Long userId) {
        UserSyncDTO syncMessage = new UserSyncDTO(
            "USER_DELETED",
            userId,
            null,
            null
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published USER_DELETED event for user ID: {}", userId);
    }

    public void publishUserDeviceMapping(Long userId, Long deviceId) {
        UserSyncDTO syncMessage = new UserSyncDTO(
            "USER_DEVICE_MAPPING",
            userId,
            null,
            deviceId
        );
        
        rabbitTemplate.convertAndSend(syncQueueName, syncMessage);
        log.info("Published USER_DEVICE_MAPPING event: user {} -> device {}", userId, deviceId);
    }
}
