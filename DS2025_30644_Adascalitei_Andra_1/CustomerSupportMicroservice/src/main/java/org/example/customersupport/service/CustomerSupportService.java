package org.example.customersupport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customersupport.dto.ChatMessageDto;
import org.example.customersupport.dto.ChatMessageRequest;
import org.example.customersupport.entity.ChatMessage;
import org.example.customersupport.entity.ChatSession;
import org.example.customersupport.repository.ChatMessageRepository;
import org.example.customersupport.repository.ChatSessionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSupportService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatbotRuleService chatbotRuleService;
    private final AICustomerSupportService aiCustomerSupportService;
    private final RabbitTemplate rabbitTemplate;
    
    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final String CHAT_ROUTING_KEY = "chat.message";
    
    @Transactional
    public ChatMessageDto processUserMessage(ChatMessageRequest request) {
        log.info("Processing user message from user: {} in session: {}", request.getUserId(), request.getSessionId());
        
        // Get or create session
        ChatSession session = getOrCreateSession(request.getSessionId(), request.getUserId(), request.getUsername());
        
        // Save user message
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getSessionId());
        userMessage.setUserId(request.getUserId());
        userMessage.setUsername(request.getUsername());
        userMessage.setMessage(request.getMessage());
        userMessage.setSenderType(ChatMessage.SenderType.USER);
        userMessage.setIsAutomated(false);
        
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);
        
        // Send user message to WebSocket immediately
        sendMessageToWebSocket(ChatMessageDto.fromEntity(savedUserMessage));
        
        // Check if session is waiting for admin or admin has taken over
        if (session.getStatus() == ChatSession.SessionStatus.WAITING_FOR_ADMIN) {
            // Don't send AI response, just notify that admin will respond
            ChatMessage waitingMessage = new ChatMessage();
            waitingMessage.setSessionId(session.getSessionId());
            waitingMessage.setUserId(request.getUserId());
            waitingMessage.setUsername("System");
            waitingMessage.setMessage("Your message has been received. An administrator will respond shortly.");
            waitingMessage.setSenderType(ChatMessage.SenderType.SYSTEM);
            waitingMessage.setIsAutomated(true);
            waitingMessage.setRuleMatched("WAITING_FOR_ADMIN");
            
            ChatMessage savedWaitingMessage = chatMessageRepository.save(waitingMessage);
            sendMessageToWebSocket(ChatMessageDto.fromEntity(savedWaitingMessage));
            
            // Update session timestamp
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            
            return ChatMessageDto.fromEntity(savedWaitingMessage);
        }
        
        // If admin has taken over, don't send AI response or waiting message - admin will respond
        if (session.getStatus() == ChatSession.SessionStatus.ADMIN_ACTIVE) {
            // Just update session timestamp and return user message - no automated responses
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            
            return ChatMessageDto.fromEntity(savedUserMessage);
        }
        
        // Only use AI/rules if session is ACTIVE and no admin has taken over
        if (session.getStatus() == ChatSession.SessionStatus.ACTIVE) {
            // Get conversation history for AI context
            String conversationHistory = getConversationHistory(session.getSessionId(), 5);
            
            // AI first, if it doesn't work fall back to rules
            String botResponse;
            String matchedRule;
            
            if (aiCustomerSupportService.isAIEnabled()) {
                log.info("Using AI-powered customer support for message: {}", request.getMessage());
                botResponse = aiCustomerSupportService.processMessage(request.getMessage(), conversationHistory);
                matchedRule = aiCustomerSupportService.getMatchedRule(request.getMessage());
                
                // If AI fails, fall back to rule-based system
                if (botResponse == null) {
                    log.warn("AI service failed, falling back to rule-based system");
                    botResponse = chatbotRuleService.processMessage(request.getMessage());
                    matchedRule = "AI_FALLBACK: " + chatbotRuleService.getMatchedRule(request.getMessage());
                }
            } else {
                log.info("Using rule-based customer support for message: {}", request.getMessage());
                botResponse = chatbotRuleService.processMessage(request.getMessage());
                matchedRule = chatbotRuleService.getMatchedRule(request.getMessage());
            }
            
            // Save bot response
            ChatMessage botMessage = new ChatMessage();
            botMessage.setSessionId(session.getSessionId());
            botMessage.setUserId(request.getUserId());
            botMessage.setUsername("Support Bot");
            botMessage.setMessage(botResponse);
            botMessage.setSenderType(ChatMessage.SenderType.SYSTEM);
            botMessage.setIsAutomated(true);
            botMessage.setRuleMatched(matchedRule);
            
            ChatMessage savedBotMessage = chatMessageRepository.save(botMessage);
            
            // Update session
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            
            // Send to WebSocket using RabbitMQ
            sendMessageToWebSocket(ChatMessageDto.fromEntity(savedBotMessage));
            
            log.info("Bot responded with rule: {} for message: {}", matchedRule, request.getMessage());
            
            return ChatMessageDto.fromEntity(savedBotMessage);
        }
        
        // If session is closed or in unknown state, just acknowledge
        return ChatMessageDto.fromEntity(savedUserMessage);
    }
    
    private String getConversationHistory(String sessionId, int maxMessages) {
        List<ChatMessage> recentMessages = chatMessageRepository
            .findTop10BySessionIdOrderByTimestampDesc(sessionId)
            .stream()
            .limit(maxMessages)
            .toList();
        
        if (recentMessages.isEmpty()) {
            return "";
        }
        
        StringBuilder history = new StringBuilder();
        // chronological order
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            String sender = msg.getSenderType() == ChatMessage.SenderType.USER ? "User" : "Assistant";
            history.append(sender).append(": ").append(msg.getMessage()).append("\n");
        }
        
        return history.toString();
    }
    
    public boolean isAIEnabled() {
        return aiCustomerSupportService.isAIEnabled();
    }
    
    @Transactional
    public void requestAdminSupport(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setStatus(ChatSession.SessionStatus.WAITING_FOR_ADMIN);
            chatSessionRepository.save(session);
            
            // Create and save a user-facing notification message
            ChatMessage userNotification = new ChatMessage();
            userNotification.setSessionId(sessionId);
            userNotification.setUserId(session.getUserId());
            userNotification.setUsername("System");
            userNotification.setMessage("Admin support has been requested. An administrator will join the conversation shortly.");
            userNotification.setSenderType(ChatMessage.SenderType.SYSTEM);
            userNotification.setIsAutomated(true);
            userNotification.setRuleMatched("ADMIN_REQUEST_USER");
            
            ChatMessage savedUserNotification = chatMessageRepository.save(userNotification);
            
            // Send user notification using WebSocket
            sendMessageToWebSocket(ChatMessageDto.fromEntity(savedUserNotification));
            
            // Create and save the admin notification message
            ChatMessage adminNotification = new ChatMessage();
            adminNotification.setSessionId(sessionId);
            adminNotification.setUserId(session.getUserId());
            adminNotification.setUsername("System");
            adminNotification.setMessage("User " + session.getUsername() + " is requesting admin support");
            adminNotification.setSenderType(ChatMessage.SenderType.SYSTEM);
            adminNotification.setIsAutomated(true);
            adminNotification.setRuleMatched("ADMIN_REQUEST_NOTIFICATION");
            
            ChatMessage savedAdminNotification = chatMessageRepository.save(adminNotification);
            
            // Send admin notification via WebSocket (this could go to a different topic for admins only)
            sendMessageToWebSocket(ChatMessageDto.fromEntity(savedAdminNotification));
            
            log.info("Admin support requested for session: {}", sessionId);
        }
    }
    
    @Transactional
    public void takeOverSession(String sessionId, Long adminId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setStatus(ChatSession.SessionStatus.ADMIN_ACTIVE);
            chatSessionRepository.save(session);
            
            // Send clear handover notification
            ChatMessageDto takeoverNotification = new ChatMessageDto();
            takeoverNotification.setSessionId(sessionId);
            takeoverNotification.setUserId(session.getUserId());
            takeoverNotification.setUsername("System");
            takeoverNotification.setMessage("🔄 An administrator has joined the conversation. The AI assistant is now disabled and you will be speaking directly with a human support agent.");
            takeoverNotification.setSenderType(ChatMessage.SenderType.SYSTEM);
            takeoverNotification.setTimestamp(LocalDateTime.now());
            takeoverNotification.setIsAutomated(true);
            takeoverNotification.setRuleMatched("ADMIN_TAKEOVER");
            
            // Save the notification to database
            ChatMessage takeoverMessage = new ChatMessage();
            takeoverMessage.setSessionId(sessionId);
            takeoverMessage.setUserId(session.getUserId());
            takeoverMessage.setUsername("System");
            takeoverMessage.setMessage(takeoverNotification.getMessage());
            takeoverMessage.setSenderType(ChatMessage.SenderType.SYSTEM);
            takeoverMessage.setIsAutomated(true);
            takeoverMessage.setRuleMatched("ADMIN_TAKEOVER");
            chatMessageRepository.save(takeoverMessage);
            
            sendMessageToWebSocket(takeoverNotification);
            log.info("Admin {} took over session: {}", adminId, sessionId);
        }
    }
    
    @Transactional
    public ChatMessageDto sendAdminMessage(ChatMessageRequest request) {
        log.info("Processing admin message in session: {}", request.getSessionId());
        
        // Get session
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(request.getSessionId());
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Chat session not found: " + request.getSessionId());
        }
        
        ChatSession session = sessionOpt.get();
        
        // Save admin message
        ChatMessage adminMessage = new ChatMessage();
        adminMessage.setSessionId(session.getSessionId());
        adminMessage.setUserId(request.getUserId());
        adminMessage.setUsername(request.getUsername());
        adminMessage.setMessage(request.getMessage());
        adminMessage.setSenderType(ChatMessage.SenderType.ADMIN);
        adminMessage.setIsAutomated(false);
        
        ChatMessage savedMessage = chatMessageRepository.save(adminMessage);
        
        // Update session
        session.setUpdatedAt(LocalDateTime.now());

        chatSessionRepository.save(session);
        
        // Send to WebSocket via RabbitMQ
        sendMessageToWebSocket(ChatMessageDto.fromEntity(savedMessage));
        
        return ChatMessageDto.fromEntity(savedMessage);
    }
    
    public List<ChatMessageDto> getChatHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<ChatSession> getActiveSessions() {
        // Include both ACTIVE and WAITING_FOR_ADMIN sessions for admin dashboard
        List<ChatSession> activeSessions = chatSessionRepository.findByStatusOrderByCreatedAtDesc(ChatSession.SessionStatus.ACTIVE);
        List<ChatSession> waitingSessions = chatSessionRepository.findByStatusOrderByCreatedAtDesc(ChatSession.SessionStatus.WAITING_FOR_ADMIN);
        List<ChatSession> adminActiveSessions = chatSessionRepository.findByStatusOrderByCreatedAtDesc(ChatSession.SessionStatus.ADMIN_ACTIVE);
        
        // Combine all active session types
        activeSessions.addAll(waitingSessions);
        activeSessions.addAll(adminActiveSessions);
        
        // Sort by creation date descending (most recent first)
        activeSessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return activeSessions;
    }
    
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Transactional
    public void closeSession(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setStatus(ChatSession.SessionStatus.CLOSED);
            session.setClosedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            log.info("Closed chat session: {}", sessionId);
        }
    }
    
    private ChatSession getOrCreateSession(String sessionId, Long userId, String username) {
        if (sessionId != null) {
            Optional<ChatSession> existingSession = chatSessionRepository.findBySessionId(sessionId);
            if (existingSession.isPresent()) {
                return existingSession.get();
            }
        }
        
        // Create new session
        ChatSession newSession = new ChatSession();
        newSession.setSessionId(sessionId != null ? sessionId : UUID.randomUUID().toString());
        newSession.setUserId(userId);
        newSession.setUsername(username);
        newSession.setStatus(ChatSession.SessionStatus.ACTIVE);
        
        return chatSessionRepository.save(newSession);
    }
    
    private void sendMessageToWebSocket(ChatMessageDto message) {
        try {
            rabbitTemplate.convertAndSend(CHAT_EXCHANGE, CHAT_ROUTING_KEY, message);
            log.debug("Sent message to WebSocket via RabbitMQ: {}", message.getMessage());
        } catch (Exception e) {
            log.error("Failed to send message to WebSocket: {}", e.getMessage(), e);
        }
    }
}