package com.sportscenter.chat.ai;

import java.util.List;

public interface AiCompletionClient {
    AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens);
}
