package org.example.customersupport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatbotRuleService {
    
    private final Map<String, String> exactMatchRules;
    private final Map<Pattern, String> patternRules;
    
    public ChatbotRuleService() {
        this.exactMatchRules = new LinkedHashMap<>();
        this.patternRules = new LinkedHashMap<>();
        initializeRules();
    }
    
    private void initializeRules() {
        // Rule 1: Greeting
        patternRules.put(Pattern.compile("(?i).*(hello|hi|hey|good morning|good afternoon|good evening).*"), 
            "Hello! Welcome to Energy Management System support. How can I help you today?");
        
        // Rule 2: Device related
        patternRules.put(Pattern.compile("(?i).*(device|devices).*"), 
            "For device-related issues: You can manage your devices from the dashboard. If you need to add a new device, contact your administrator. For device consumption data, check the monitoring section.");
        
        // Rule 3: Energy consumption
        patternRules.put(Pattern.compile("(?i).*(energy|consumption|power|usage|kwh).*"), 
            "Energy consumption data is available in your dashboard. You can view hourly, daily, and monthly consumption patterns. If you notice unusual consumption, check your device settings or contact support.");
        
        // Rule 4: Login issues
        patternRules.put(Pattern.compile("(?i).*(login|password|forgot|access|sign in).*"), 
            "For login issues: 1) Check your username and password, 2) Clear browser cache, 3) Try incognito mode. If problems persist, contact your administrator for password reset.");
        
        // Rule 5: Dashboard navigation
        patternRules.put(Pattern.compile("(?i).*(dashboard|navigate|menu|interface).*"), 
            "Dashboard navigation: Use the main menu to access Users, Devices, and Monitoring sections. Admin users have additional management options. Click on any device to view detailed consumption data.");
        
        // Rule 6: Charts and graphs
        patternRules.put(Pattern.compile("(?i).*(chart|graph|visualization|data|statistics).*"), 
            "Charts and visualizations: Select a device and date to view consumption charts. You can toggle between line and bar charts. Data is displayed hourly from 00:00 to 23:00.");
        
        // Rule 7: Alerts and notifications
        patternRules.put(Pattern.compile("(?i).*(alert|notification|warning|overconsumption).*"), 
            "Alerts and notifications: You'll receive real-time notifications for overconsumption events. These appear automatically when device usage exceeds the maximum threshold.");
        
        // Rule 8: User management
        patternRules.put(Pattern.compile("(?i).*(user|account|profile|manage users).*"), 
            "User management: Administrators can create, update, and delete user accounts from the Users section. Regular users can view their profile but cannot modify other accounts.");
        
        // Rule 9: Technical issues
        patternRules.put(Pattern.compile("(?i).*(error|bug|problem|issue|not working|broken).*"), 
            "Technical issues: 1) Refresh the page, 2) Check your internet connection, 3) Try a different browser. For persistent issues, please describe the exact error message you're seeing.");
        
        // Rule 10: System requirements
        patternRules.put(Pattern.compile("(?i).*(requirement|browser|system|compatibility).*"), 
            "System requirements: The system works best with modern browsers (Chrome, Firefox, Safari, Edge). Ensure JavaScript is enabled and you have a stable internet connection.");
        
        // Rule 11: Data export
        patternRules.put(Pattern.compile("(?i).*(export|download|save|report).*"), 
            "Data export: Currently, you can view consumption data in charts. For detailed reports or data export features, please contact your administrator.");
        
        // Rule 12: Contact information
        patternRules.put(Pattern.compile("(?i).*(contact|support|help|admin|administrator).*"), 
            "Contact support: For issues not resolved by this chatbot, you can reach out to your system administrator. They have access to advanced troubleshooting and account management tools.");
        
        // Rule 13: Goodbye/Thank you
        patternRules.put(Pattern.compile("(?i).*(bye|goodbye|thank you|thanks|see you).*"), 
            "Thank you for using Energy Management System support! If you need further assistance, feel free to ask. Have a great day!");
        
        // Rule 14: Default fallback - MUST BE LAST
        patternRules.put(Pattern.compile(".*"), 
            "I'm here to help with Energy Management System questions. You can ask me about devices, energy consumption, login issues, dashboard navigation, or technical problems. What would you like to know?");
    }
    
    public String processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Please provide a message so I can assist you better.";
        }
        
        String normalizedMessage = message.trim();
        log.debug("Processing message: {}", normalizedMessage);
        
        // Check exact matches first
        for (Map.Entry<String, String> entry : exactMatchRules.entrySet()) {
            if (normalizedMessage.equalsIgnoreCase(entry.getKey())) {
                log.debug("Exact match found for: {}", entry.getKey());
                return entry.getValue();
            }
        }
        
        // Check pattern matches
        for (Map.Entry<Pattern, String> entry : patternRules.entrySet()) {
            if (entry.getKey().matcher(normalizedMessage).matches()) {
                log.debug("Pattern match found: {}", entry.getKey().pattern());
                return entry.getValue();
            }
        }
        
        // just in case
        return "I'm sorry, I didn't understand that. Could you please rephrase your question?";
    }
    
    public String getMatchedRule(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "EMPTY_MESSAGE";
        }
        
        String normalizedMessage = message.trim();
        
        // Check exact matches first
        for (String key : exactMatchRules.keySet()) {
            if (normalizedMessage.equalsIgnoreCase(key)) {
                return "EXACT_MATCH: " + key;
            }
        }
        
        // Check pattern matches
        for (Pattern pattern : patternRules.keySet()) {
            if (pattern.matcher(normalizedMessage).matches()) {
                return "PATTERN_MATCH: " + pattern.pattern();
            }
        }
        
        return "NO_MATCH";
    }
}