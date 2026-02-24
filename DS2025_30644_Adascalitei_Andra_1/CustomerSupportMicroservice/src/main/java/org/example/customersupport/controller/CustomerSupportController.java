package org.example.customersupport.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customersupport.dto.ChatMessageDto;
import org.example.customersupport.dto.ChatMessageRequest;
import org.example.customersupport.entity.ChatSession;
import org.example.customersupport.service.CustomerSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomerSupportController {
    
    private final CustomerSupportService customerSupportService;
    
    @PostMapping("/chat/user")
    public ResponseEntity<ChatMessageDto> sendUserMessage(@RequestBody ChatMessageRequest request) {
        log.info("Received user message: {}", request.getMessage());
        ChatMessageDto response = customerSupportService.processUserMessage(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/chat/admin")
    public ResponseEntity<ChatMessageDto> sendAdminMessage(@RequestBody ChatMessageRequest request) {
        log.info("Received admin message: {}", request.getMessage());
        ChatMessageDto response = customerSupportService.sendAdminMessage(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable("sessionId") String sessionId) {
        log.info("Fetching chat history for session: {}", sessionId);
        List<ChatMessageDto> history = customerSupportService.getChatHistory(sessionId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/sessions/active")
    public ResponseEntity<List<ChatSession>> getActiveSessions() {
        log.info("Fetching active chat sessions");
        List<ChatSession> sessions = customerSupportService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<List<ChatSession>> getUserSessions(@PathVariable("userId") Long userId) {
        log.info("Fetching sessions for user: {}", userId);
        List<ChatSession> sessions = customerSupportService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }
    
    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<Void> closeSession(@PathVariable("sessionId") String sessionId) {
        log.info("Closing session: {}", sessionId);
        customerSupportService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/request-admin")
    public ResponseEntity<Void> requestAdminSupport(@PathVariable("sessionId") String sessionId) {
        log.info("Requesting admin support for session: {}", sessionId);
        customerSupportService.requestAdminSupport(sessionId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/take-over")
    public ResponseEntity<Void> takeOverSession(@PathVariable("sessionId") String sessionId, @RequestParam("adminId") Long adminId) {
        log.info("Admin {} taking over session: {}", adminId, sessionId);
        customerSupportService.takeOverSession(sessionId, adminId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Customer Support Microservice is running");
    }
    
    @GetMapping("/ai/status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("aiEnabled", customerSupportService.isAIEnabled());
        status.put("mode", customerSupportService.isAIEnabled() ? "AI-Powered" : "Rule-Based");
        return ResponseEntity.ok(status);
    }
}