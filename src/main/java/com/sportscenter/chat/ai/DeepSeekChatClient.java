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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "deepseek")
@ConditionalOnExpression("'${scms.ai.chat.deepseek-api-key:}'.trim().length() > 0")
public class DeepSeekChatClient implements AiCompletionClient {
    private final RestClient restClient;
    private final ChatAiProperties properties;

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
}
