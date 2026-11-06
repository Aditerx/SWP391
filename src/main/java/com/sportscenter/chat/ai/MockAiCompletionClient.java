package com.sportscenter.chat.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiCompletionClient implements AiCompletionClient {
    private final ChatAiProperties properties;

    public MockAiCompletionClient(ChatAiProperties properties) { this.properties = properties; }

    @Override
    public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        return new AiCompletion(properties.getMockReply(), "mock", 0, 0);
    }
}
