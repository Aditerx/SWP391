package com.sportscenter.chat.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "faq")
public class FaqAiCompletionClient implements AiCompletionClient {
    private static final String REPLY = "Xin ch\u00e0o! M\u00ecnh l\u00e0 tr\u1ee3 l\u00fd SCMS. B\u1ea1n mu\u1ed1n t\u00ecm hi\u1ec3u th\u00f4ng tin g\u00ec?";
    @Override public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        return new AiCompletion(REPLY, "faq", 0, 0);
    }
}
