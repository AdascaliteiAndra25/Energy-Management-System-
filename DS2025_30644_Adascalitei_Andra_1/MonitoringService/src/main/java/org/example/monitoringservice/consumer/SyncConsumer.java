package org.example.monitoringservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.monitoringservice.entity.User;
import org.example.monitoringservice.repository.UserRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncConsumer {

    private final UserRepository userRepository;

    @RabbitListener(queues = "${sync.queue.name}")
    public void consumeSyncMessage(Object syncMessage) {
        log.info("Received sync message: {}", syncMessage);
        
        if (syncMessage instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> messageMap = (java.util.Map<String, Object>) syncMessage;
            String eventType = (String) messageMap.get("eventType");



            switch (eventType) {
                case "USER_CREATED":
                    handleUserCreated(messageMap);
                    break;
                case "USER_UPDATED":
                    handleUserUpdated(messageMap);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(messageMap);
                    break;
                case "USER_DEVICE_MAPPING":
                    handleUserDeviceMapping(messageMap);
                    break;
                case "DEVICE_CREATED":
                    handleDeviceCreated(messageMap);
                    break;
                case "DEVICE_UPDATED":
                    handleDeviceUpdated(messageMap);
                    break;
                case "DEVICE_DELETED":
                    handleDeviceDeleted(messageMap);
                    break;
                case "DEVICE_USER_MAPPING":
                    handleDeviceUserMapping(messageMap);
                    break;
                default:
                    log.info("Unhandled event type: {}", eventType);
            }
        }
    }
    
    private void handleUserCreated(java.util.Map<String, Object> messageMap) {
        Long userId = ((Number) messageMap.get("userId")).longValue();
        String username = (String) messageMap.get("username");
        log.info("User created: {} (ID: {})", username, userId);

    }
    
    private void handleUserUpdated(java.util.Map<String, Object> messageMap) {
        Long userId = ((Number) messageMap.get("userId")).longValue();
        String username = (String) messageMap.get("username");
        log.info("User updated: {} (ID: {})", username, userId);
        

        userRepository.findById(userId).ifPresent(user -> {
            log.info("Updated user info in monitoring: userId={}", userId);
        });
    }
    
    private void handleUserDeleted(java.util.Map<String, Object> messageMap) {
        Long userId = ((Number) messageMap.get("userId")).longValue();
        log.info("User deleted: {}", userId);

        userRepository.deleteById(userId);
        log.info("Deleted user-device mappings for userId={}", userId);
    }
    
    private void handleUserDeviceMapping(java.util.Map<String, Object> messageMap) {
        Long userId = ((Number) messageMap.get("userId")).longValue();
        Long deviceId = ((Number) messageMap.get("deviceId")).longValue();
        
        User user = new User();
        user.setId(userId);
        user.setDeviceId(deviceId);
        userRepository.save(user);
        log.info("Saved user-device mapping: userId={}, deviceId={}", userId, deviceId);
    }
    
    private void handleDeviceCreated(java.util.Map<String, Object> messageMap) {
        Long deviceId = ((Number) messageMap.get("deviceId")).longValue();
        String deviceName = (String) messageMap.get("deviceName");
        Object userIdObj = messageMap.get("userId");
        
        log.info("Device created: {} (ID: {})", deviceName, deviceId);
        
        if (userIdObj != null) {
            Long userId = ((Number) userIdObj).longValue();
            User user = new User();
            user.setId(userId);
            user.setDeviceId(deviceId);
            userRepository.save(user);
            log.info("Auto-created user-device mapping from device creation: userId={}, deviceId={}", userId, deviceId);
        }
    }
    
    private void handleDeviceUpdated(java.util.Map<String, Object> messageMap) {
        Long deviceId = ((Number) messageMap.get("deviceId")).longValue();
        String deviceName = (String) messageMap.get("deviceName");
        Object userIdObj = messageMap.get("userId");
        
        log.info("Device updated: {} (ID: {})", deviceName, deviceId);
        
        if (userIdObj != null) {
            Long userId = ((Number) userIdObj).longValue();
            User user = new User();
            user.setId(userId);
            user.setDeviceId(deviceId);
            userRepository.save(user);
            log.info("Updated user-device mapping: userId={}, deviceId={}", userId, deviceId);
        }
    }
    
    private void handleDeviceDeleted(java.util.Map<String, Object> messageMap) {
        Long deviceId = ((Number) messageMap.get("deviceId")).longValue();
        log.info("Device deleted: {}", deviceId);

        userRepository.findAll().stream()
            .filter(user -> deviceId.equals(user.getDeviceId()))
            .forEach(user -> {
                userRepository.delete(user);
                log.info("Deleted user-device mapping for deviceId={}, userId={}", deviceId, user.getId());
            });
    }
    
    private void handleDeviceUserMapping(java.util.Map<String, Object> messageMap) {
        Long deviceId = ((Number) messageMap.get("deviceId")).longValue();
        Long userId = ((Number) messageMap.get("userId")).longValue();
        
        User user = new User();
        user.setId(userId);
        user.setDeviceId(deviceId);
        userRepository.save(user);
        log.info("Saved device-user mapping: deviceId={}, userId={}", deviceId, userId);
    }
}
