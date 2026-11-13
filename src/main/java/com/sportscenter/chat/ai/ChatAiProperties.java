package com.sportscenter.chat.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConfigurationProperties(prefix = "scms.ai.chat")
public class ChatAiProperties {
    private static final Logger log = LoggerFactory.getLogger(ChatAiProperties.class);
    private boolean enabled;
    private String newConversationHandler = "STAFF";
    private String handoffMode = "SUGGEST";
    private String provider = "mock";
    private String model = "";
    private String apiKey = "";
    private String baseUrl = "https://api.anthropic.com";
    private String apiVersion = "2026-10-21";
    private int timeoutSeconds = 12;
    private int maxTokens = 400;
    private int maxInputCharacters = 8000;
    private int maxKnowledgeBaseCharacters = 12000;
    private double temperature = 0.2;
    private int dailyLimitPerMember = 30;
    private int faqMatchThreshold = 2;
    private int faqMaxAnswerCharacters = 1200;
    private int faqFallbackEscalationThreshold = 2;
    private String faqSensitiveKeywords = "ho\u00e0n ti\u1ec1n,refund,\u0111\u1ed5i l\u1ecbch,h\u1ee7y g\u00f3i,h\u1ee7y bu\u1ed5i,thanh to\u00e1n,h\u00f3a \u0111\u01a1n,khi\u1ebfu n\u1ea1i,t\u00e0i kho\u1ea3n,s\u1ef1 c\u1ed1 s\u1ee9c kh\u1ecfe";
    private String deepseekBaseUrl = "https://api.deepseek.com";
    private String deepseekModel = "deepseek-flash";
    private String deepseekApiKey = "";
    private int deepseekTimeoutSeconds = 12;
    private int deepseekMaxTokens = 400;
    private boolean dbToolsEnabled;
    private boolean markdownEnabled;
    private int toolMaxRounds = 3;
    private int toolMaxRows = 10;
    private String mockReply = "Xin ch\u00e0o! T\u00f4i l\u00e0 tr\u1ee3 l\u00fd h\u1ed7 tr\u1ee3 SCMS. T\u00f4i c\u00f3 th\u1ec3 gi\u00fap b\u1ea1n t\u00ecm hi\u1ec3u d\u1ecbch v\u1ee5, l\u1ecbch t\u1eadp, g\u00f3i t\u1eadp v\u00e0 ch\u00ednh s\u00e1ch c\u1ee7a trung t\u00e2m. V\u1edbi th\u00f4ng tin t\u00e0i kho\u1ea3n ho\u1eb7c y\u00eau c\u1ea7u c\u1ea7n x\u1eed l\u00fd, vui l\u00f2ng ch\u1ecdn g\u1eb7p nh\u00e2n vi\u00ean.";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getNewConversationHandler() { return newConversationHandler; }
    public void setNewConversationHandler(String value) { this.newConversationHandler = value; }
    public String getHandoffMode() { return handoffMode; }
    public void setHandoffMode(String value) { this.handoffMode = value; normalizeHandoffMode(); }
    public boolean isSuggestHandoff() { return !"AUTO".equalsIgnoreCase(handoffMode); }
    @PostConstruct
    void normalizeHandoffMode() {
        if ("AUTO".equalsIgnoreCase(handoffMode)) handoffMode = "AUTO";
        else if (handoffMode == null || handoffMode.isBlank() || "SUGGEST".equalsIgnoreCase(handoffMode)) handoffMode = "SUGGEST";
        else {
            log.warn("Invalid scms.ai.chat.handoff-mode='{}'; using SUGGEST", handoffMode);
            handoffMode = "SUGGEST";
        }
    }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getMaxInputCharacters() { return maxInputCharacters; }
    public void setMaxInputCharacters(int maxInputCharacters) { this.maxInputCharacters = maxInputCharacters; }
    public String getMockReply() { return mockReply; }
    public void setMockReply(String mockReply) { this.mockReply = mockReply; }
    public int getMaxKnowledgeBaseCharacters() { return maxKnowledgeBaseCharacters; }
    public void setMaxKnowledgeBaseCharacters(int value) { this.maxKnowledgeBaseCharacters = value; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double value) { this.temperature = value; }
    public int getDailyLimitPerMember() { return dailyLimitPerMember; }
    public void setDailyLimitPerMember(int value) { this.dailyLimitPerMember = value; }
    public int getFaqMatchThreshold() { return faqMatchThreshold; }
    public void setFaqMatchThreshold(int value) { this.faqMatchThreshold = value; }
    public int getFaqMaxAnswerCharacters() { return faqMaxAnswerCharacters; }
    public void setFaqMaxAnswerCharacters(int value) { this.faqMaxAnswerCharacters = value; }
    public int getFaqFallbackEscalationThreshold() { return faqFallbackEscalationThreshold; }
    public void setFaqFallbackEscalationThreshold(int value) { this.faqFallbackEscalationThreshold = value; }
    public String getFaqSensitiveKeywords() { return faqSensitiveKeywords; }
    public void setFaqSensitiveKeywords(String value) { this.faqSensitiveKeywords = value; }
    public String getDeepseekBaseUrl() { return deepseekBaseUrl; }
    public void setDeepseekBaseUrl(String value) { this.deepseekBaseUrl = value; }
    public String getDeepseekModel() { return deepseekModel; }
    public void setDeepseekModel(String value) { this.deepseekModel = value; }
    public String getDeepseekApiKey() { return deepseekApiKey == null ? "" : deepseekApiKey.trim(); }
    public void setDeepseekApiKey(String value) { this.deepseekApiKey = value == null ? "" : value.trim(); }
    public int getDeepseekTimeoutSeconds() { return deepseekTimeoutSeconds; }
    public void setDeepseekTimeoutSeconds(int value) { this.deepseekTimeoutSeconds = value; }
    public int getDeepseekMaxTokens() { return deepseekMaxTokens; }
    public void setDeepseekMaxTokens(int value) { this.deepseekMaxTokens = value; }
    public boolean isDbToolsEnabled() { return dbToolsEnabled; }
    public void setDbToolsEnabled(boolean value) { this.dbToolsEnabled = value; }
    public boolean isMarkdownEnabled() { return markdownEnabled; }
    public void setMarkdownEnabled(boolean value) { this.markdownEnabled = value; }
    public int getToolMaxRounds() { return Math.max(1, Math.min(5, toolMaxRounds)); }
    public void setToolMaxRounds(int value) { this.toolMaxRounds = value; }
    public int getToolMaxRows() { return Math.max(1, Math.min(25, toolMaxRows)); }
    public void setToolMaxRows(int value) { this.toolMaxRows = value; }
}
