package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeepSeekChatClientTest {
    @Test void mockAndFaqProvidersNeverCreateDeepseekHttpBeansAndMissingKeyUsesLocalFallback() {
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=mock", "scms.ai.chat.deepseek.api-key=")
                .run(context -> {
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertFalse(context.containsBean("deepSeekChatClient"));
                });
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=faq", "scms.ai.chat.deepseek.api-key=")
                .run(context -> {
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertFalse(context.containsBean("deepSeekChatClient"));
                });
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=deepseek", "scms.ai.chat.deepseek.api-key=")
                .run(context -> {
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertFalse(context.containsBean("deepSeekChatClient"));
                    assertTrue(context.containsBean("deepSeekMissingKeyClient"));
                });
    }

    @Test void sendsOpenAiCompatibleRequestAndParsesUsage() throws Exception {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(builder.requestFactory(factory).baseUrl("https://api.deepseek.com").defaultHeader("Authorization", "Bearer unit-test-key").defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/chat/completions")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(new ObjectMapper().readTree("{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"content\":\"Hello\"}}],\"usage\":{\"prompt_tokens\":17,\"completion_tokens\":5}}"));
        ChatAiProperties properties = new ChatAiProperties(); properties.setDeepseekApiKey("unit-test-key");
        DeepSeekChatClient client = new DeepSeekChatClient(builder, factory, properties);
        AiCompletion result = client.complete("system", List.of(new AiChatMessage("user", "question")), 91);
        assertEquals("Hello", result.text());
        assertEquals(17, result.inputTokens());
        assertEquals(5, result.outputTokens());
        verify(builder.requestFactory(factory).baseUrl("https://api.deepseek.com"), atLeastOnce()).defaultHeader("Authorization", "Bearer unit-test-key");
        verify(uriSpec).uri("/chat/completions");
        org.mockito.ArgumentCaptor<Object> body = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(bodySpec).body(body.capture());
        Map<?, ?> request = (Map<?, ?>) body.getValue();
        assertEquals("deepseek-flash", request.get("model"));
        assertEquals(91, request.get("max_tokens"));
        assertEquals(0.2, request.get("temperature"));
        assertEquals(Map.of("type", "disabled"), request.get("thinking"));
        assertEquals(List.of(Map.of("role", "system", "content", "system"), Map.of("role", "user", "content", "question")), request.get("messages"));
    }

    @Test void retriesServerErrorOnlyOnce() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(builder.requestFactory(factory).baseUrl(anyString()).defaultHeader(anyString(), anyString()).defaultHeader(anyString(), anyString()).build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenThrow(responseError(503));
        ChatAiProperties properties = new ChatAiProperties(); properties.setDeepseekApiKey("key");
        DeepSeekChatClient client = new DeepSeekChatClient(builder, factory, properties);
        assertThrows(AiProviderException.class, () -> client.complete("system", List.of(new AiChatMessage("user", "x")), 10));
        verify(responseSpec, times(2)).body(JsonNode.class);
    }

    @Test void doesNotRetryAuthenticationBalanceOrRateLimitButRetriesTimeoutOnce() {
        for (int status : List.of(401, 402, 429)) {
            RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
            RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
            RestClient.RequestBodyUriSpec uri = mock(RestClient.RequestBodyUriSpec.class);
            RestClient clientMock = mock(RestClient.class);
            RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
            ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
            when(builder.requestFactory(factory).baseUrl(anyString()).defaultHeader(anyString(), anyString()).defaultHeader(anyString(), anyString()).build()).thenReturn(clientMock);
            when(clientMock.post()).thenReturn(uri); when(uri.uri(anyString())).thenReturn(body);
            when(body.contentType(any())).thenReturn(body); when(body.body(any(Object.class))).thenReturn(body); when(body.retrieve()).thenReturn(response);
            when(response.body(JsonNode.class)).thenThrow(responseError(status));
            ChatAiProperties properties = new ChatAiProperties(); properties.setDeepseekApiKey("key");
            DeepSeekChatClient api = new DeepSeekChatClient(builder, factory, properties);
            assertThrows(AiProviderException.class, () -> api.complete("system", List.of(new AiChatMessage("user", "x")), 10));
            verify(response, times(1)).body(JsonNode.class);
        }

        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        RestClient.RequestBodyUriSpec uri = mock(RestClient.RequestBodyUriSpec.class);
        RestClient clientMock = mock(RestClient.class);
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
        when(builder.requestFactory(factory).baseUrl(anyString()).defaultHeader(anyString(), anyString()).defaultHeader(anyString(), anyString()).build()).thenReturn(clientMock);
        when(clientMock.post()).thenReturn(uri); when(uri.uri(anyString())).thenReturn(body);
        when(body.contentType(any())).thenReturn(body); when(body.body(any(Object.class))).thenReturn(body); when(body.retrieve()).thenReturn(response);
        when(response.body(JsonNode.class)).thenThrow(new ResourceAccessException("timeout"));
        ChatAiProperties properties = new ChatAiProperties(); properties.setDeepseekApiKey("key");
        DeepSeekChatClient api = new DeepSeekChatClient(builder, factory, properties);
        assertThrows(AiProviderException.class, () -> api.complete("system", List.of(new AiChatMessage("user", "x")), 10));
        verify(response, times(2)).body(JsonNode.class);
    }

    private RestClientResponseException responseError(int status) { return new RestClientResponseException("error", status, "error", null, null, null); }
}
