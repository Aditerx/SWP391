package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.ClientHttpRequestFactory;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "anthropic")
public class AnthropicMessagesClient implements AiCompletionClient {
    private final RestClient restClient;
    private final ChatAiProperties properties;

    public AnthropicMessagesClient(RestClient.Builder builder, ClientHttpRequestFactory anthropicRequestFactory, ChatAiProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .requestFactory(anthropicRequestFactory)
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-api-key", properties.getApiKey())
                .defaultHeader("anthropic-version", properties.getApiVersion())
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiCompletion complete(String systemPrompt, List<AiChatMessage> messages, int maxTokens) {
        String model = properties.getModel();
        if (properties.getApiKey() == null || properties.getApiKey().isBlank() || model == null || model.isBlank()) {
            throw new AiProviderException();
        }
        Map<String, Object> payload = Map.of("model", model, "max_tokens", maxTokens,
                "system", systemPrompt, "messages", messages.stream()
                        .map(message -> Map.of("role", message.role(), "content", message.content())).toList());
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                JsonNode response = restClient.post().uri("/v1/messages").contentType(MediaType.APPLICATION_JSON)
                        .body(payload).retrieve().body(JsonNode.class);
                if (response == null) throw new AiProviderException();
                StringBuilder text = new StringBuilder();
                JsonNode content = response.path("content");
                if (content.isArray()) for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        if (!text.isEmpty()) text.append('\n');
                        text.append(block.path("text").asText());
                    }
                }
                JsonNode usage = response.path("usage");
                if (text.isEmpty()) throw new AiProviderException();
                return new AiCompletion(text.toString(), response.path("model").asText(model),
                        usage.has("input_tokens") ? usage.path("input_tokens").asInt() : null,
                        usage.has("output_tokens") ? usage.path("output_tokens").asInt() : null);
            } catch (ResourceAccessException ex) {
                if (attempt == 1) throw new AiProviderException();
            } catch (RestClientResponseException ex) {
                if (attempt == 1 || ex.getStatusCode().is4xxClientError() || ex.getStatusCode().value() < 500) {
                    throw new AiProviderException();
                }
            }
        }
        throw new AiProviderException();
    }
}
