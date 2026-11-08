package com.sportscenter.chat.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "faq")
public class FaqAiCompletionClient implements AiCompletionClient {
    private static final String REPLY = "Xin ch\u00e0o! Hi\u1ec7n t\u00f4i ch\u01b0a c\u00f3 th\u00f4ng tin ph\u00f9 h\u1ee3p trong t\u00e0i li\u1ec7u. T\u00f4i s\u1ebd chuy\u1ec3n c\u00e2u h\u1ecfi \u0111\u1ebfn nh\u00e2n vi\u00ean \u0111\u1ec3 h\u1ed7 tr\u1ee3 b\u1ea1n. [HANDOFF]";
    @Override public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        return new AiCompletion(REPLY, "faq", 0, 0);
    }
}
