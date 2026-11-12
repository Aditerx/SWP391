package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.sportscenter.chat.ai.tool.AiTool;
import com.sportscenter.chat.ai.tool.AiToolRegistry;
import com.sportscenter.chat.ai.tool.ToolAiCompletion;
import com.sportscenter.chat.ai.tool.ToolCapableAiCompletionClient;
import com.sportscenter.chat.ai.tool.ToolContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "deepseek")
@ConditionalOnExpression("'${scms.ai.chat.deepseek-api-key:}'.trim().length() > 0")
public class DeepSeekChatClient implements AiCompletionClient, ToolCapableAiCompletionClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatClient.class);
    private final RestClient restClient;
    private final ChatAiProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekChatClient(RestClient.Builder builder, @Qualifier("deepseekRequestFactory") ClientHttpRequestFactory requestFactory, ChatAiProperties properties) {
        this.properties = properties;
        this.restClient = builder.requestFactory(requestFactory).baseUrl(properties.getDeepseekBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getDeepseekApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Override
    public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        List<Map<String, String>> turns = new ArrayList<>();
        turns.add(Map.of("role", "system", "content", systemPrompt));
        messages.forEach(message -> turns.add(Map.of("role", message.role(), "content", message.content())));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getDeepseekModel());
        payload.put("messages", turns);
        payload.put("max_tokens", Math.max(1, Math.min(maxTokens, properties.getDeepseekMaxTokens())));
        payload.put("temperature", Math.max(0.0, Math.min(0.4, properties.getTemperature())));
        payload.put("thinking", Map.of("type", "disabled"));
        payload.put("stream", false);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                JsonNode response = restClient.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                        .body(payload).retrieve().body(JsonNode.class);
                JsonNode choice = response == null ? null : response.path("choices").path(0);
                String text = choice == null ? null : choice.path("message").path("content").asText(null);
                if (text == null || text.isBlank()) throw new AiProviderException("invalid_response");
                JsonNode usage = response.path("usage");
                return new AiCompletion(text, response.path("model").asText(properties.getDeepseekModel()),
                        usage.has("prompt_tokens") ? usage.path("prompt_tokens").asInt() : null,
                        usage.has("completion_tokens") ? usage.path("completion_tokens").asInt() : null);
            } catch (ResourceAccessException ex) {
                if (attempt == 1) throw new AiProviderException("timeout_or_io");
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status >= 500 && attempt == 0) continue;
                throw new AiProviderException("http_" + status);
            }
        }
        throw new AiProviderException("unavailable");
    }

    @Override
    public ToolAiCompletion completeWithTools(String systemPrompt, List<AiChatMessage> messages,
            int maxTokens, ToolContext context, int maxRounds) {
        AiToolRegistry registry = this.toolRegistry;
        if (registry == null) throw new AiProviderException("tools_unavailable");
        List<Object> turns = new ArrayList<>();
        turns.add(Map.of("role", "system", "content", systemPrompt));
        messages.forEach(message -> turns.add(Map.of("role", message.role(), "content", message.content())));
        List<Map<String,Object>> definitions = new ArrayList<>();
        for (AiTool tool : registry.all()) definitions.add(Map.of("type", "function", "function", Map.of(
                "name", tool.name(), "description", tool.description(), "parameters", tool.parametersSchema())));
        int inputTokens = 0, outputTokens = 0;
        boolean hasInputTokens = false, hasOutputTokens = false;
        String model = properties.getDeepseekModel();
        List<String> calledTools = new ArrayList<>();
        int rounds = 0;
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(Math.max(1, properties.getDeepseekTimeoutSeconds()) * (long)(Math.max(1, maxRounds) + 1)).toNanos();
        while (true) {
            if (System.nanoTime() >= deadline) throw new AiProviderException("tool_total_timeout");
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("model", model); payload.put("messages", turns);
            payload.put("max_tokens", Math.max(1, Math.min(maxTokens, properties.getDeepseekMaxTokens())));
            payload.put("temperature", Math.max(0.0, Math.min(0.4, properties.getTemperature())));
            payload.put("thinking", Map.of("type", "disabled")); payload.put("stream", false);
            payload.put("tools", definitions); payload.put("tool_choice", "auto");
            JsonNode response = post(payload);
            if (System.nanoTime() >= deadline) throw new AiProviderException("tool_total_timeout");
            JsonNode choice = response.path("choices").path(0);
            JsonNode message = choice.path("message");
            JsonNode usage = response.path("usage");
            if (usage.has("prompt_tokens")) { inputTokens += usage.path("prompt_tokens").asInt(); hasInputTokens = true; }
            if (usage.has("completion_tokens")) { outputTokens += usage.path("completion_tokens").asInt(); hasOutputTokens = true; }
            model = response.path("model").asText(model);
            if (!"tool_calls".equals(choice.path("finish_reason").asText())) {
                String text = message.path("content").asText(null);
                if (text == null || text.isBlank()) throw new AiProviderException("invalid_response");
                return new ToolAiCompletion(new AiCompletion(text, model, hasInputTokens ? inputTokens : null,
                        hasOutputTokens ? outputTokens : null), calledTools, rounds);
            }
            if (rounds >= Math.max(1, maxRounds)) throw new AiProviderException("tool_round_limit");
            JsonNode calls = message.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty() || calls.size() > 6) throw new AiProviderException("invalid_tool_calls");
            turns.add(Map.of("role", "assistant", "content", message.path("content").isNull() ? "" : message.path("content").asText(), "tool_calls", calls));
            for (JsonNode call : calls) {
                String id = call.path("id").asText(null);
                String name = call.path("function").path("name").asText(null);
                String raw = call.path("function").path("arguments").asText(null);
                if (id == null || id.length() > 120 || name == null || raw == null || raw.length() > 4000)
                    throw new AiProviderException("invalid_tool_call");
                try {
                    JsonNode args = mapper.readTree(raw);
                    String result = registry.execute(name, context, args);
                    if (result == null || result.length() > 2000) throw new IllegalArgumentException("tool_result_limit");
                    calledTools.add(name);
                    turns.add(Map.of("role", "tool", "tool_call_id", id, "content", result));
                } catch (Exception ex) {
                    log.warn("AI_CHAT_TOOL_FAILED tool={} sqlState={} exceptionType={}", safeToolName(name),
                            sqlState(ex), ex.getClass().getSimpleName(), ex);
                    throw new AiProviderException(ex instanceof IllegalArgumentException ? "invalid_tool_arguments" : "tool_failed");
                }
            }
            rounds++;
        }
    }

    private AiToolRegistry toolRegistry;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setToolRegistry(AiToolRegistry registry) { this.toolRegistry = registry; }

    private String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof SQLException sql && sql.getSQLState() != null) return sql.getSQLState();
        return "none";
    }

    private String safeToolName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,80}") ? name : "invalid";
    }

    private JsonNode post(Map<String,Object> payload) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                JsonNode response = restClient.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                        .body(payload).retrieve().body(JsonNode.class);
                if (response == null || !response.path("choices").isArray() || response.path("choices").isEmpty())
                    throw new AiProviderException("invalid_response");
                return response;
            } catch (ResourceAccessException ex) {
                if (attempt == 1) throw new AiProviderException("timeout_or_io");
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status >= 500 && attempt == 0) continue;
                throw new AiProviderException("http_" + status);
            }
        }
        throw new AiProviderException("unavailable");
    }
}
