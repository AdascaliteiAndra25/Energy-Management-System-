package org.example.websocketmicroservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.websocketmicroservice.dto.ChatMessageDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "chat.queue")
    public void handleChatMessage(ChatMessageDto chatMessage) {
        try {
            log.info("Received chat message from RabbitMQ: {}", chatMessage);
            
            // Send to specific user's chat session
            String destination = "/topic/chat/" + chatMessage.getSessionId();
            messagingTemplate.convertAndSend(destination, chatMessage);
            
            // Send to admin channels based on message type
            if ("USER".equals(chatMessage.getSenderType().toString())) {
                // User messages go to general admin chat monitoring
                messagingTemplate.convertAndSend("/topic/admin/chat", chatMessage);
            } else if ("SYSTEM".equals(chatMessage.getSenderType().toString()) && 
                      "ADMIN_REQUEST".equals(chatMessage.getRuleMatched())) {
                // Admin support requests go to admin notifications
                messagingTemplate.convertAndSend("/topic/admin/notifications", chatMessage);
                messagingTemplate.convertAndSend("/topic/admin/chat", chatMessage);
            } else if ("ADMIN".equals(chatMessage.getSenderType().toString())) {
                // Admin messages go to admin chat monitoring
                messagingTemplate.convertAndSend("/topic/admin/chat", chatMessage);
            }
            
            log.info("Forwarded chat message to WebSocket destination: {}", destination);
            
        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
        }
    }
}