package com.sportscenter.chat.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "deepseek")
@ConditionalOnExpression("'${scms.ai.chat.deepseek.api-key:}' == ''")
public class DeepSeekMissingKeyClient implements AiCompletionClient {
    @Override public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        return new AiCompletion("Tro ly AI chua duoc cau hinh. Toi se chuyen ban den nhan vien de ho tro. [HANDOFF]", "faq-fallback", 0, 0);
    }
}
