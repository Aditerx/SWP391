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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import java.io.InputStream;
import java.util.Properties;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.sportscenter.chat.ai.tool.AiTool;
import com.sportscenter.chat.ai.tool.AiToolRegistry;
import com.sportscenter.chat.ai.tool.ToolContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeepSeekChatClientTest {
    @Test void rejectsAdditionalToolRoundBeyondConfiguredLimit() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"model\":\"deepseek-flash\",\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"read\",\"arguments\":\"{}\"}}]}}]}";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ChatAiProperties properties = new ChatAiProperties();
            properties.setDeepseekApiKey("test-key");
            properties.setDeepseekBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            DeepSeekChatClient client = new DeepSeekChatClient(RestClient.builder(), new org.springframework.http.client.SimpleClientHttpRequestFactory(), properties);
            AiTool tool = mock(AiTool.class);
            when(tool.name()).thenReturn("read");
            when(tool.description()).thenReturn("read");
            when(tool.parametersSchema()).thenReturn(new ObjectMapper().createObjectNode().put("type", "object"));
            AiToolRegistry registry = mock(AiToolRegistry.class);
            when(registry.all()).thenReturn(List.of(tool));
            when(registry.execute(eq("read"), any(), any())).thenReturn("[]");
            client.setToolRegistry(registry);
            assertThrows(AiProviderException.class, () -> client.completeWithTools("s", List.of(new AiChatMessage("user", "q")), 20, new ToolContext(7), 1));
            verify(registry, times(1)).execute(eq("read"), any(), any());
        } finally { server.stop(0); }
    }

    @Test void toolCallsAreExecutedOnlyByRegistryAndUsageIsAccumulated() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<String> secondRequest = new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int count = calls.incrementAndGet();
            if (count == 2) secondRequest.set(request);
            String body = count == 1
                    ? "{\"model\":\"deepseek-flash\",\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"get_my_subscriptions\",\"arguments\":\"{}\"}}]}}],\"usage\":{\"prompt_tokens\":11,\"completion_tokens\":2}}"
                    : "{\"model\":\"deepseek-flash\",\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"You have one subscription.\"}}],\"usage\":{\"prompt_tokens\":13,\"completion_tokens\":4}}";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ChatAiProperties properties = new ChatAiProperties();
            properties.setDeepseekApiKey("integration-key");
            properties.setDeepseekBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            DeepSeekChatClient client = new DeepSeekChatClient(RestClient.builder(), new org.springframework.http.client.SimpleClientHttpRequestFactory(), properties);
            AiTool tool = mock(AiTool.class);
            when(tool.name()).thenReturn("get_my_subscriptions");
            when(tool.description()).thenReturn("subscriptions");
            when(tool.parametersSchema()).thenReturn(new ObjectMapper().createObjectNode().put("type", "object"));
            AiToolRegistry registry = mock(AiToolRegistry.class);
            when(registry.all()).thenReturn(List.of(tool));
            when(registry.execute(eq("get_my_subscriptions"), any(), any())).thenReturn("[]");
            client.setToolRegistry(registry);

            var result = client.completeWithTools("system", List.of(new AiChatMessage("user", "my subscriptions")), 40,
                    new ToolContext(123), 3);

            assertEquals("You have one subscription.", result.completion().text());
            assertEquals(24, result.completion().inputTokens());
            assertEquals(6, result.completion().outputTokens());
            assertEquals(List.of("get_my_subscriptions"), result.tools());
            assertEquals(1, result.toolRounds());
            assertTrue(secondRequest.get().contains("\"role\":\"tool\""));
            assertTrue(secondRequest.get().contains("\"tool_call_id\":\"call-1\""));
            verify(registry).execute(eq("get_my_subscriptions"), eq(new ToolContext(123)), any());
        } finally { server.stop(0); }
    }

    @Test void bindsFlatConfigurationAndSendsTrimmedAuthorizationHeader() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicReference<String> authorization = new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"content\":\"Hello\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            new ApplicationContextRunner()
                    .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class, TestRestClientConfiguration.class)
                    .withPropertyValues("scms.ai.chat.provider=deepseek", "scms.ai.chat.deepseek-api-key=  integration-key  ",
                            "scms.ai.chat.deepseek-base-url=http://127.0.0.1:" + server.getAddress().getPort())
                    .run(context -> {
                        assertEquals("integration-key", context.getBean(ChatAiProperties.class).getDeepseekApiKey());
                        assertTrue(context.containsBean("deepSeekChatClient"));
                        assertFalse(context.containsBean("deepSeekMissingKeyClient"));
                        AiCompletion result = context.getBean(DeepSeekChatClient.class).complete("system", List.of(new AiChatMessage("user", "question")), 20);
                        assertEquals("Hello", result.text());
                    });
            assertEquals("Bearer integration-key", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void whitespaceKeyCreatesOnlyMissingKeyClient() {
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class, TestRestClientConfiguration.class)
                .withPropertyValues("scms.ai.chat.provider=deepseek", "scms.ai.chat.deepseek-api-key=   ")
                .run(context -> {
                    assertEquals("", context.getBean(ChatAiProperties.class).getDeepseekApiKey());
                    assertFalse(context.containsBean("deepSeekChatClient"));
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertTrue(context.containsBean("deepSeekMissingKeyClient"));
                });
    }

    @Test void everyChatAiApplicationPropertyHasAChatAiPropertiesField() throws Exception {
        Properties values = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) { values.load(input); }
        values.replaceAll((key, value) -> value.toString().replaceAll("\\$\\{[^:}]+:([^}]*)}", "$1"));
        for (String key : values.stringPropertyNames()) {
            if (!key.startsWith("scms.ai.chat.")) continue;
            String property = key.substring("scms.ai.chat.".length());
            String fieldName = java.util.Arrays.stream(property.split("[-.]"))
                    .map(part -> part.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + part.substring(1))
                    .collect(java.util.stream.Collectors.joining());
            String matchingField = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
            assertDoesNotThrow(() -> ChatAiProperties.class.getDeclaredField(matchingField), key + " must have a matching field");
        }
        var source = new PropertiesPropertySource("application", values);
        ChatAiProperties bound = new Binder(ConfigurationPropertySources.from(source)).bind("scms.ai.chat", ChatAiProperties.class).get();
        assertEquals("https://api.deepseek.com", bound.getDeepseekBaseUrl());
        assertEquals("deepseek-flash", bound.getDeepseekModel());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatAiProperties.class)
    static class TestRestClientConfiguration {
        @Bean RestClient.Builder restClientBuilder() { return RestClient.builder(); }
    }

    @Test void mockAndFaqProvidersNeverCreateDeepseekHttpBeansAndMissingKeyUsesLocalFallback() {
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=mock", "scms.ai.chat.deepseek-api-key=")
                .run(context -> {
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertFalse(context.containsBean("deepSeekChatClient"));
                });
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=faq", "scms.ai.chat.deepseek-api-key=")
                .run(context -> {
                    assertFalse(context.containsBean("deepseekRequestFactory"));
                    assertFalse(context.containsBean("deepSeekChatClient"));
                });
        new ApplicationContextRunner()
                .withUserConfiguration(DeepSeekHttpConfiguration.class, DeepSeekChatClient.class, DeepSeekMissingKeyClient.class)
                .withPropertyValues("scms.ai.chat.provider=deepseek", "scms.ai.chat.deepseek-api-key=")
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
