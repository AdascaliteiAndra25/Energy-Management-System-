package org.example.customersupport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AICustomerSupportService {
    
    private final RestTemplate restTemplate;
    
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;
    
    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;
    
    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    
    public AICustomerSupportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public boolean isAIEnabled() {
        return (aiEnabled && geminiApiKey != null && !geminiApiKey.trim().isEmpty()) );
    }
    
    public String processMessage(String userMessage, String conversationHistory) {
        if (!isAIEnabled()) {
            return null; // Fall back to rule-based system
        }
        
        try {

            
            // Use real Gemini API
            if (aiEnabled && geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
                log.info("Using Google Gemini API service");
                return callGemini(userMessage, conversationHistory);
            }
            
            return null; // Fall back to rule-based system
            
        } catch (Exception e) {
            log.error("Error calling AI service: {}", e.getMessage(), e);
            return null; // Fall back to rule-based system
        }
    }
    
    private String buildSystemPrompt() {
        return """
            You are a helpful customer support assistant for an Energy Management System. 
            Your role is to help users with:
            
            1. Device Management: Adding, configuring, and monitoring energy devices
            2. Energy Consumption: Understanding usage patterns, viewing consumption data
            3. Dashboard Navigation: How to use the interface, access different sections
            4. Login Issues: Password resets, access problems, authentication
            5. Technical Problems: Troubleshooting errors, browser compatibility
            6. User Management: Account settings, profile management (for admins)
            7. Alerts & Notifications: Understanding overconsumption warnings
            8. Data Visualization: Reading charts, graphs, and reports
            
            Guidelines:
            - Be concise but helpful (max 3-4 sentences)
            - Provide step-by-step instructions when needed
            - If you don't know something specific about the system, suggest contacting an administrator
            - Always maintain a professional and friendly tone
            - Focus on practical solutions
            - If the user asks about features not mentioned above, politely explain the system's capabilities
            
            The system includes:
            - User dashboard with device monitoring
            - Real-time energy consumption tracking
            - Overconsumption alerts via WebSocket notifications
            - Admin panel for user and device management
            - Charts showing hourly consumption data (00:00 to 23:00)
            - Role-based access (Admin and Regular users)
            """;
    }
    
    private String callGemini(String userMessage, String conversationHistory) {
        String url = GEMINI_API_URL.replace("{model}", model) + "?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Build the prompt with system instructions and conversation history
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(buildSystemPrompt()).append("\n\n");
        
        // Add conversation history if available
        if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
            fullPrompt.append("Previous conversation:\n").append(conversationHistory).append("\n\n");
        }
        
        fullPrompt.append("User question: ").append(userMessage).append("\n\n");
        fullPrompt.append("Please provide a helpful response:");
        
        // Build Gemini API request body
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", fullPrompt.toString());
        content.put("parts", List.of(part));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        
        // Add generation config for better responses
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 500);
        requestBody.put("generationConfig", generationConfig);
        
        // Add safety settings
        List<Map<String, Object>> safetySettings = List.of(
            Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
            Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
        );
        requestBody.put("safetySettings", safetySettings);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> content1 = (Map<String, Object>) firstCandidate.get("content");
                    if (content1 != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content1.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
            
            log.warn("Unexpected response from Gemini API: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Failed to call Gemini API: {}", e.getMessage());
            return null;
        }
    }
    
    public String getMatchedRule(String message) {
         if (aiEnabled && geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            return "GEMINI_AI_POWERED";
        } else {
            return "AI_DISABLED";
        }
    }
}