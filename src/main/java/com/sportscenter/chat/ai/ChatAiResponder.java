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
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.enabled", havingValue = "true")
public class ChatAiResponder implements ChatResponder {
    private static final Logger log = LoggerFactory.getLogger(ChatAiResponder.class);
    private final AiCompletionClient client;
    private final ChatAiProperties properties;
    private final ObjectMapper objectMapper;
    private final FaqKnowledgeBase knowledgeBase = new FaqKnowledgeBase();

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
        String memberText = recentMessages.stream().filter(message -> message.getSenderType() == ChatSenderType.MEMBER)
                .reduce((first, second) -> second).map(ChatMessage::getContent).orElse("");
        long started = System.nanoTime();
        String systemPrompt = systemPrompt();
        AiCompletion result;
        FaqDecision faqDecision = null;
        if (client instanceof FaqAiCompletionClient) {
            faqDecision = faqDecision(memberText, recentMessages);
            result = new AiCompletion(faqDecision.text(), "faq", 0, 0);
        } else try {
            result = client.complete(systemPrompt, List.copyOf(prompt), Math.max(1, properties.getMaxTokens()));
        } catch (AiProviderException ex) {
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            log.warn("AI_CHAT_FALLBACK model={} latencyMs={} errorType={}", properties.getDeepseekModel(), latencyMs, ex.getMessage().substring("AI provider unavailable: ".length()));
            faqDecision = faqDecision(memberText, recentMessages);
            result = new AiCompletion(faqDecision.text(), "faq-fallback", null, null);
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
        if (faqDecision != null) {
            if (faqDecision.fallback()) metadata.put("fallback", true);
            if (faqDecision.reasonCode() != null) metadata.put("reasonCode", faqDecision.reasonCode());
        }
        try {
            return Optional.of(new ChatResponderReply(result.text().trim(), objectMapper.writeValueAsString(metadata)));
        } catch (JsonProcessingException ex) {
            return Optional.of(new ChatResponderReply(result.text().trim(), "{}"));
        }
    }

    private FaqDecision faqDecision(String memberText, List<ChatMessage> recentMessages) {
        String normalized = FaqKnowledgeBase.normalize(memberText);
        String topics = knowledgeBase.topicList();
        if (isExplicitStaffRequest(normalized)) {
            return new FaqDecision("M\u00ecnh s\u1ebd chuy\u1ec3n cu\u1ed9c tr\u00f2 chuy\u1ec7n cho nh\u00e2n vi\u00ean h\u1ed7 tr\u1ee3 ngay. [HANDOFF]", false, "MEMBER_REQUESTED_STAFF");
        }
        if (containsConfiguredKeyword(normalized, properties.getFaqSensitiveKeywords())) {
            String text = properties.isSuggestHandoff()
                    ? "Y\u00eau c\u1ea7u n\u00e0y th\u01b0\u1eddng c\u1ea7n nh\u00e2n vi\u00ean h\u1ed7 tr\u1ee3 tr\u1ef1c ti\u1ebfp. B\u1ea1n c\u00f3 mu\u1ed1n chuy\u1ec3n sang nh\u00e2n vi\u00ean kh\u00f4ng? B\u1ea1n c\u00f3 th\u1ec3 b\u1ea5m 'G\u1eb7p nh\u00e2n vi\u00ean' b\u1ea5t c\u1ee9 l\u00fac n\u00e0o, ho\u1eb7c ti\u1ebfp t\u1ee5c h\u1ecfi m\u00ecnh."
                    : "Y\u00eau c\u1ea7u n\u00e0y c\u1ea7n nh\u00e2n vi\u00ean h\u1ed7 tr\u1ee3 tr\u1ef1c ti\u1ebfp. M\u00ecnh s\u1ebd chuy\u1ec3n cu\u1ed9c tr\u00f2 chuy\u1ec7n ngay.";
            return new FaqDecision(text + " [HANDOFF]", false, "SENSITIVE_TOPIC");
        }
        if (isGreetingOrThanksOrGoodbye(normalized)) {
            String greeting = normalized.equals("tam biet") || normalized.equals("bye") || normalized.equals("goodbye")
                    ? "T\u1ea1m bi\u1ec7t! Ch\u00fac b\u1ea1n m\u1ed9t ng\u00e0y vui v\u1ebb."
                    : normalized.contains("cam on") || normalized.equals("thanks") || normalized.equals("thank you")
                    ? "Kh\u00f4ng c\u00f3 g\u00ec! M\u00ecnh lu\u00f4n s\u1eb5n s\u00e0ng h\u1ed7 tr\u1ee3 b\u1ea1n."
                    : "Xin ch\u00e0o! M\u00ecnh l\u00e0 tr\u1ee3 l\u00fd SCMS. M\u00ecnh c\u00f3 th\u1ec3 gi\u00fap b\u1ea1n v\u1ec1: " + topics + ". B\u1ea1n mu\u1ed1n h\u1ecfi g\u00ec?";
            return new FaqDecision(greeting, false, null);
        }
        Optional<String> answer = knowledgeBase.answer(memberText, properties.getFaqMatchThreshold(), properties.getFaqMaxAnswerCharacters());
        if (answer.isPresent()) return new FaqDecision(answer.get(), false, null);

        int consecutiveFallbacks = consecutiveFallbacks(recentMessages);
        boolean escalate = consecutiveFallbacks + 1 >= Math.max(1, properties.getFaqFallbackEscalationThreshold());
        String unmatched = "M\u00ecnh ch\u01b0a c\u00f3 th\u00f4ng tin v\u1ec1 n\u1ed9i dung n\u00e0y. B\u1ea1n c\u00f3 th\u1ec3 h\u1ecfi v\u1ec1: " + topics
                + ", ho\u1eb7c b\u1ea5m 'G\u1eb7p nh\u00e2n vi\u00ean' \u0111\u1ec3 \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3 tr\u1ef1c ti\u1ebfp.";
        return new FaqDecision(unmatched + (escalate ? " [HANDOFF]" : ""), true,
                escalate ? "REPEATED_KB_MISS" : null);
    }

    private int consecutiveFallbacks(List<ChatMessage> recentMessages) {
        int count = 0;
        for (int index = recentMessages.size() - 1; index >= 0; index--) {
            ChatMessage message = recentMessages.get(index);
            if (message.getSenderType() == ChatSenderType.MEMBER) continue;
            if (message.getSenderType() != ChatSenderType.AI || message.getMetadata() == null) break;
            try {
                if (!objectMapper.readTree(message.getMetadata()).path("fallback").asBoolean(false)) break;
                count++;
            } catch (JsonProcessingException ignored) { break; }
        }
        return count;
    }

    private boolean containsConfiguredKeyword(String normalized, String configured) {
        if (configured == null) return false;
        return java.util.Arrays.stream(configured.split(","))
                .map(FaqKnowledgeBase::normalize).filter(value -> !value.isBlank())
                .anyMatch(value -> (" " + normalized + " ").contains(" " + value + " "));
    }

    public static boolean isExplicitStaffRequest(String text) {
        String normalized = FaqKnowledgeBase.normalize(text);
        return List.of("gap nhan vien", "gap nguoi that", "muon gap nhan vien", "cho toi gap nhan vien",
                "noi chuyen voi nhan vien", "can nhan vien").stream()
                .anyMatch(phrase -> (" " + normalized + " ").contains(" " + phrase + " "));
    }

    private boolean isGreetingOrThanksOrGoodbye(String normalized) {
        if (List.of("hello", "hi", "hey", "alo", "thanks", "bye", "goodbye").contains(normalized)) return true;
        return List.of("xin chao", "chao", "cam on", "tam biet", "hen gap lai", "thank you")
                .stream().anyMatch(phrase -> (" " + normalized + " ").contains(" " + phrase + " "));
    }

    private record FaqDecision(String text, boolean fallback, String reasonCode) { }

    private String systemPrompt() {
        String knowledge;
        try {
            knowledge = FaqKnowledgeBase.repairMojibake(new ClassPathResource("ai/knowledge-base.md").getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            knowledge = "Ch\u01b0a c\u00f3 th\u00f4ng tin \u0111\u00e3 duy\u1ec7t. Kh\u00f4ng suy \u0111o\u00e1n; \u0111\u1ec1 ngh\u1ecb th\u00e0nh vi\u00ean g\u1eb7p nh\u00e2n vi\u00ean.";
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
            prompt = "Tr\u1ea3 l\u1eddi b\u1eb1ng ti\u1ebfng Vi\u1ec7t. Ch\u1ec9 d\u00f9ng knowledge base; n\u1ebfu thi\u1ebfu th\u00f4ng tin, \u0111\u1ec1 ngh\u1ecb g\u1eb7p nh\u00e2n vi\u00ean. [HANDOFF]";
        }
        return prompt + "\n\nKNOWLEDGE BASE:\n" + knowledge;
    }
}
