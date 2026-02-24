package org.example.customersupport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.customersupport.entity.ChatMessage;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    
    private Long id;
    private String sessionId;
    private Long userId;
    private String username;
    private String message;
    private ChatMessage.SenderType senderType;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Boolean isAutomated;
    private String ruleMatched;
    
    public static ChatMessageDto fromEntity(ChatMessage chatMessage) {
        return new ChatMessageDto(
            chatMessage.getId(),
            chatMessage.getSessionId(),
            chatMessage.getUserId(),
            chatMessage.getUsername(),
            chatMessage.getMessage(),
            chatMessage.getSenderType(),
            chatMessage.getTimestamp(),
            chatMessage.getIsAutomated(),
            chatMessage.getRuleMatched()
        );
    }
}