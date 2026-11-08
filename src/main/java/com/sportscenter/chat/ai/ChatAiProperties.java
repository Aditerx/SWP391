package com.sportscenter.chat.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.ai.chat")
public class ChatAiProperties {
    private boolean enabled;
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
    private String deepseekBaseUrl = "https://api.deepseek.com";
    private String deepseekModel = "deepseek-flash";
    private String deepseekApiKey = "";
    private int deepseekTimeoutSeconds = 12;
    private int deepseekMaxTokens = 400;
    private String mockReply = "Xin ch\u00e0o! T\u00f4i l\u00e0 tr\u1ee3 l\u00fd h\u1ed7 tr\u1ee3 SCMS. T\u00f4i c\u00f3 th\u1ec3 gi\u00fap b\u1ea1n t\u00ecm hi\u1ec3u d\u1ecbch v\u1ee5, l\u1ecbch t\u1eadp, g\u00f3i t\u1eadp v\u00e0 ch\u00ednh s\u00e1ch c\u1ee7a trung t\u00e2m. V\u1edbi th\u00f4ng tin t\u00e0i kho\u1ea3n ho\u1eb7c y\u00eau c\u1ea7u c\u1ea7n x\u1eed l\u00fd, vui l\u00f2ng ch\u1ecdn g\u1eb7p nh\u00e2n vi\u00ean.";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
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
    public String getDeepseekBaseUrl() { return deepseekBaseUrl; }
    public void setDeepseekBaseUrl(String value) { this.deepseekBaseUrl = value; }
    public String getDeepseekModel() { return deepseekModel; }
    public void setDeepseekModel(String value) { this.deepseekModel = value; }
    public String getDeepseekApiKey() { return deepseekApiKey; }
    public void setDeepseekApiKey(String value) { this.deepseekApiKey = value; }
    public int getDeepseekTimeoutSeconds() { return deepseekTimeoutSeconds; }
    public void setDeepseekTimeoutSeconds(int value) { this.deepseekTimeoutSeconds = value; }
    public int getDeepseekMaxTokens() { return deepseekMaxTokens; }
    public void setDeepseekMaxTokens(int value) { this.deepseekMaxTokens = value; }
}
