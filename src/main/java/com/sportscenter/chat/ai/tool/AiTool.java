package com.sportscenter.chat.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface AiTool {
    String name();
    String description();
    JsonNode parametersSchema();
    String execute(ToolContext context, JsonNode arguments);
}
