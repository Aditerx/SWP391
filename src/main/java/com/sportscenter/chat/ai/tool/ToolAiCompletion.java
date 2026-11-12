package com.sportscenter.chat.ai.tool;

import com.sportscenter.chat.ai.AiCompletion;
import java.util.List;

public record ToolAiCompletion(AiCompletion completion, List<String> tools, int toolRounds) {
    public ToolAiCompletion { tools = List.copyOf(tools); }
}
