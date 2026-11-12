package com.sportscenter.chat.ai.tool;

import com.sportscenter.chat.ai.AiChatMessage;
import java.util.List;

public interface ToolCapableAiCompletionClient {
    ToolAiCompletion completeWithTools(String systemPrompt, List<AiChatMessage> messages,
            int maxTokens, ToolContext context, int maxRounds);
}
