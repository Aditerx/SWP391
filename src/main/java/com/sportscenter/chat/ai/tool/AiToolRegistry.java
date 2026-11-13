package com.sportscenter.chat.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AiToolRegistry {
    private final Map<String, AiTool> tools;
    public AiToolRegistry(ScmsReadOnlyAiTools tools) {
        Map<String, AiTool> indexed = new LinkedHashMap<>();
        for (AiTool tool : tools.all()) {
            if (indexed.putIfAbsent(tool.name(), tool) != null) throw new IllegalStateException("Duplicate AI tool: " + tool.name());
        }
        this.tools = Map.copyOf(indexed);
    }
    public List<AiTool> all() { return List.copyOf(tools.values()); }
    public AiTool require(String name) {
        AiTool tool = tools.get(name);
        if (tool == null) throw new IllegalArgumentException("unknown_tool");
        return tool;
    }
    public String execute(String name, ToolContext context, JsonNode arguments) {
        return require(name).execute(context, arguments);
    }
}
