package com.sportscenter.chat.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.chat.ChatConversation;
import com.sportscenter.chat.ChatMessage;
import com.sportscenter.chat.ChatResponder;
import com.sportscenter.chat.ChatResponderReply;
import com.sportscenter.chat.ChatSenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.enabled", havingValue = "true")
public class ChatAiResponder implements ChatResponder {
    private static final Logger log = LoggerFactory.getLogger(ChatAiResponder.class);
    private final AiCompletionClient client;
    private final ChatAiProperties properties;
    private final ObjectMapper objectMapper;

    public ChatAiResponder(AiCompletionClient client, ChatAiProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override public boolean supports(ChatConversation conversation) { return properties.isEnabled(); }

    @Override
    public Optional<ChatResponderReply> respond(ChatConversation conversation, List<ChatMessage> recentMessages, Integer memberUserId) {
        List<AiChatMessage> prompt = new ArrayList<>();
        int remaining = Math.max(1, properties.getMaxInputCharacters());
        for (int index = recentMessages.size() - 1; index >= 0; index--) {
            ChatMessage message = recentMessages.get(index);
            String role = switch (message.getSenderType()) {
                case MEMBER -> "user";
                case AI, STAFF -> "assistant";
                case SYSTEM -> null;
            };
            if (role == null || message.getContent() == null || remaining <= 0) continue;
            String content = ChatSensitiveDataFilter.redact(message.getContent());
            if (content.length() > remaining) content = content.substring(content.length() - remaining);
            remaining -= content.length();
            prompt.add(new AiChatMessage(role, content));
        }
        Collections.reverse(prompt);
        if (prompt.isEmpty()) return Optional.empty();
        long started = System.nanoTime();
        String systemPrompt = systemPrompt();
        AiCompletion result;
        try {
            result = client.complete(systemPrompt, List.copyOf(prompt), Math.max(1, properties.getMaxTokens()));
        } catch (AiProviderException ex) {
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            log.warn("AI_CHAT_FALLBACK model={} latencyMs={} errorType={}", properties.getDeepseekModel(), latencyMs, ex.getMessage().substring("AI provider unavailable: ".length()));
            result = new AiCompletion("Tro ly AI dang gap su co. Toi se chuyen cau hoi den nhan vien de ho tro ban. [HANDOFF]", "faq-fallback", null, null);
        }
        if (result == null || result.text() == null || result.text().isBlank()) return Optional.empty();
        long latencyMs = (System.nanoTime() - started) / 1_000_000;
        log.info("AI_CHAT_COMPLETION model={} latencyMs={} inputTokens={} outputTokens={}",
                result.model(), latencyMs, result.inputTokens(), result.outputTokens());
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", result.model());
        metadata.put("latencyMs", latencyMs);
        if (result.inputTokens() != null) metadata.put("inputTokens", result.inputTokens());
        if (result.outputTokens() != null) metadata.put("outputTokens", result.outputTokens());
        try {
            return Optional.of(new ChatResponderReply(result.text().trim(), objectMapper.writeValueAsString(metadata)));
        } catch (JsonProcessingException ex) {
            return Optional.of(new ChatResponderReply(result.text().trim(), "{}"));
        }
    }

    private String systemPrompt() {
        String knowledge;
        try {
            knowledge = new ClassPathResource("ai/knowledge-base.md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            knowledge = "Chua co thong tin da duyet. Khong suy doan; de nghi thanh vien gap nhan vien.";
            log.warn("AI_CHAT_KNOWLEDGE_BASE errorType={}", ex.getClass().getSimpleName());
        }
        if (knowledge.isBlank()) {
            log.warn("AI_CHAT_KNOWLEDGE_BASE_EMPTY");
        }
        int limit = Math.max(0, properties.getMaxKnowledgeBaseCharacters());
        if (knowledge.length() > limit) {
            knowledge = knowledge.substring(0, limit);
            log.warn("AI_CHAT_KNOWLEDGE_BASE_TRUNCATED limit={}", limit);
        }
        String prompt;
        try {
            prompt = new ClassPathResource("ai/system-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("AI_CHAT_SYSTEM_PROMPT errorType={}", ex.getClass().getSimpleName());
            prompt = "Tra loi bang tieng Viet. Chi dung knowledge base; neu thieu thong tin, de nghi gap nhan vien. [HANDOFF]";
        }
        return prompt + "\n\nKNOWLEDGE BASE:\n" + knowledge;
    }
}
