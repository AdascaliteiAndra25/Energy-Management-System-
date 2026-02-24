package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dtos.UserSyncDTO;
import org.example.entities.User;
import org.example.repositories.UserRepository;
import org.example.services.DeviceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncConsumer {

    private final DeviceService deviceService;
    private final UserRepository userRepository;

    @RabbitListener(queues = "${sync.queue.name}")
    public void consumeSyncMessage(UserSyncDTO syncMessage) {
        log.info("Received sync message: eventType={}, userId={}, username={}", 
                syncMessage.getEventType(), syncMessage.getUserId(), syncMessage.getUsername());
        
        switch (syncMessage.getEventType()) {
            case "USER_CREATED":
                handleUserCreated(syncMessage);
                break;
            case "USER_UPDATED":
                handleUserUpdated(syncMessage);
                break;
            case "USER_DELETED":
                handleUserDeleted(syncMessage);
                break;
            default:
                log.debug("Ignoring event type: {}", syncMessage.getEventType());
        }
    }
    
    private void handleUserCreated(UserSyncDTO dto) {
        log.info("User created event - userId: {}, username: {}", dto.getUserId(), dto.getUsername());
        
        User user = new User(dto.getUserId(), dto.getUsername());
        userRepository.save(user);
        log.info("User {} saved to local database", dto.getUserId());
    }
    
    private void handleUserUpdated(UserSyncDTO dto) {
        log.info("User updated event - userId: {}, username: {}", dto.getUserId(), dto.getUsername());
        
        userRepository.findById(dto.getUserId()).ifPresent(user -> {
            user.setUsername(dto.getUsername());
            userRepository.save(user);
            log.info("User {} updated in local database", dto.getUserId());
        });
    }
    
    private void handleUserDeleted(UserSyncDTO dto) {
        log.info("User deleted event for userId: {}", dto.getUserId());
        
        // Delete all devices for this user
        deviceService.deleteDevicesByUserId(dto.getUserId());
        
        // Delete user from local database
        userRepository.deleteById(dto.getUserId());
        log.info("Deleted all devices and user record for userId: {}", dto.getUserId());
    }
}
