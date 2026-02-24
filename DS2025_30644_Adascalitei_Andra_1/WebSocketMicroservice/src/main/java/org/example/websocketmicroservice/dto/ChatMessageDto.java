package org.example.websocketmicroservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private SenderType senderType;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Boolean isAutomated;
    private String ruleMatched;
    
    public enum SenderType {
        USER, ADMIN, SYSTEM
    }
}