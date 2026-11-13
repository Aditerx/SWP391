package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.chat.ChatConversation;
import com.sportscenter.chat.ChatMessage;
import com.sportscenter.chat.ChatResponderReply;
import com.sportscenter.chat.ChatSenderType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.sportscenter.chat.ai.tool.ToolAiCompletion;
import com.sportscenter.chat.ai.tool.ToolCapableAiCompletionClient;
import com.sportscenter.chat.ai.tool.ToolContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

class ChatAiResponderTest {
    @Test void toolPathIsOptInAndUsesAuthenticatedMemberContext() {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setEnabled(true);
        ToolClientStub client = new ToolClientStub();
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper());
        var memberMessage = message(ChatSenderType.MEMBER, "What packages do I have?");

        ChatResponderReply regular = responder.respond(new ChatConversation(), List.of(memberMessage), 42).orElseThrow();
        assertEquals("regular", regular.content());
        assertFalse(regular.metadata().contains("tools"));
        assertEquals(1, client.regularCalls);

        properties.setDbToolsEnabled(true);
        ChatResponderReply toolReply = responder.respond(new ChatConversation(), List.of(memberMessage), 42).orElseThrow();
        assertEquals("tool answer", toolReply.content());
        assertEquals(new ToolContext(42, "What packages do I have?"), client.context);
        assertTrue(toolReply.metadata().contains("\"tools\":[\"get_my_subscriptions\"]"));
        assertTrue(toolReply.metadata().contains("\"toolRounds\":1"));
        assertEquals(1, client.toolCalls);

        client.failTools = true;
        ChatResponderReply fallback = responder.respond(new ChatConversation(), List.of(memberMessage), 42).orElseThrow();
        assertTrue(fallback.content().contains("tạm thời chưa tra cứu được"));
        assertTrue(fallback.content().contains("[HANDOFF]"));
        assertTrue(fallback.metadata().contains("\"fallback\":true"));
        assertTrue(fallback.metadata().contains("AI_UNAVAILABLE"));
    }

    private static final class ToolClientStub implements AiCompletionClient, ToolCapableAiCompletionClient {
        int regularCalls;
        int toolCalls;
        ToolContext context;
        boolean failTools;
        boolean runtimeFailTools;
        String toolPrompt;
        @Override public AiCompletion complete(String system, List<AiChatMessage> messages, int maxTokens) {
            regularCalls++;
            return new AiCompletion("regular", "deepseek", 1, 1);
        }
        @Override public ToolAiCompletion completeWithTools(String system, List<AiChatMessage> messages,
                int maxTokens, ToolContext context, int maxRounds) {
            toolCalls++;
            toolPrompt = system;
            this.context = context;
            if (failTools) throw new AiProviderException("tool_failed");
            if (runtimeFailTools) throw new IllegalStateException("tool runtime failed");
            return new ToolAiCompletion(new AiCompletion("tool answer", "deepseek", 3, 2), List.of("get_my_subscriptions"), 1);
        }
    }

    @Test void toolRuntimeFailureDoesNotUseKeywordFaqAndSuggestsUnavailableHandoff() {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setEnabled(true);
        properties.setDbToolsEnabled(true);
        ToolClientStub client = new ToolClientStub();
        client.runtimeFailTools = true;
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper());
        ChatResponderReply reply = responder.respond(new ChatConversation(),
                List.of(message(ChatSenderType.MEMBER, "OTP của tôi là gì?")), 7).orElseThrow();
        assertTrue(reply.content().contains("tạm thời chưa tra cứu được"));
        assertFalse(reply.content().contains("07:00"));
        assertTrue(reply.metadata().contains("AI_RUNTIME_FAILURE"));
        assertTrue(reply.content().contains("[HANDOFF]"));
    }

    @Test void toolPromptHasCurrentHcmDateAndRequiredLookupGuidanceAndSuccessHasNoHandoff() {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setEnabled(true);
        properties.setDbToolsEnabled(true);
        ToolClientStub client = new ToolClientStub();
        Clock fixed = Clock.fixed(Instant.parse("2026-11-12T04:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper(), fixed);
        ChatResponderReply reply = responder.respond(new ChatConversation(),
                List.of(message(ChatSenderType.MEMBER, "Gói tập của tôi hết hạn ngày nào?")), 42).orElseThrow();
        assertTrue(client.toolPrompt.contains("2026-11-12T11:00"));
        assertTrue(client.toolPrompt.contains("thứ Năm"));
        assertTrue(client.toolPrompt.contains("PHẢI gọi tool tương ứng"));
        assertTrue(client.toolPrompt.contains("Không dùng bảng Markdown"));
        assertFalse(reply.metadata().contains("handoffSuggested"));
        assertFalse(reply.content().contains("Gặp nhân viên"));
    }

    @Test void sendsOnlyBoundedSanitizedTranscriptAndReturnsMetadata() throws Exception {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setEnabled(true);
        properties.setMaxInputCharacters(100);
        AiCompletionClient client = mock(AiCompletionClient.class);
        when(client.complete(anyString(), anyList(), anyInt())).thenReturn(new AiCompletion("Hello", "mock", 7, 3));
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper());
        ChatMessage member = message(ChatSenderType.MEMBER, "My password: swordfish, what are your hours?");
        ChatMessage system = message(ChatSenderType.SYSTEM, "Staff-only internal note");

        ChatResponderReply reply = responder.respond(new ChatConversation(), List.of(member, system), 777).orElseThrow();

        verify(client).complete(argThat(prompt -> prompt.contains("HANDOFF") && prompt.length() > 100), argThat(messages -> messages.size() == 1
                && messages.get(0).role().equals("user")
                && !messages.get(0).content().contains("swordfish")
                && messages.get(0).content().contains("[message omitted")), eq(400));
        assertEquals("Hello", reply.content());
        assertTrue(reply.metadata().contains("\"inputTokens\":7"));
        assertFalse(reply.metadata().contains("777"));
    }

    @Test void mockCompletionClientUsesConfiguredFixedReply() {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setMockReply("configured mock");
        AiCompletion result = new MockAiCompletionClient(properties).complete("system", List.of(), 10);
        assertEquals("configured mock", result.text());
        assertEquals("mock", result.model());
    }

    @Test void systemPromptAndKnowledgeBaseAreUtf8VietnameseWithoutMojibake() throws Exception {
        String prompt = new ClassPathResource("ai/system-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
        String knowledge = new ClassPathResource("ai/knowledge-base.md").getContentAsString(StandardCharsets.UTF_8);
        for (String text : List.of(prompt, knowledge)) {
            assertTrue(text.indexOf((char) 0x0111) >= 0 || text.indexOf((char) 0x1ec7) >= 0);
            assertFalse(hasMojibake(text));
        }
        assertTrue(prompt.contains("[HANDOFF]"));
    }

    @Test void defaultMockAndFaqRepliesContainXinChaoWithDiacritics() {
        ChatAiProperties properties = new ChatAiProperties();
        AiCompletion mockReply = new MockAiCompletionClient(properties).complete("", List.of(), 10);
        AiCompletion faqReply = new FaqAiCompletionClient().complete("", List.of(), 10);
        String welcome = "Xin ch" + (char) 0x00e0 + "o";
        assertTrue(mockReply.text().contains(welcome));
        assertTrue(faqReply.text().contains(welcome));
        assertFalse(hasMojibake(mockReply.text()));
        assertFalse(hasMojibake(faqReply.text()));
        try {
            byte[] json = new ObjectMapper().writeValueAsBytes(mockReply.text());
            String decoded = new ObjectMapper().readValue(json, String.class);
            assertEquals(mockReply.text(), decoded);
            assertTrue(decoded.contains(welcome));
        } catch (Exception ex) {
            fail(ex);
        }
    }

    @Test void applicationMockDefaultIsDecodedFromUnicodeEscapesCorrectly() throws Exception {
        var properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        String value = properties.getProperty("scms.ai.chat.mock-reply");
        assertNotNull(value);
        assertTrue(value.contains("Xin ch" + (char) 0x00e0 + "o"));
        assertFalse(hasMojibake(value));
    }

    @Test void faqGreetingAndKnowledgeMatchDoNotRequestHandoff() {
        ChatAiResponder responder = faqResponder(new ChatAiProperties());
        for (String hello : List.of("xin ch\u00e0o", "xin chao", "HELLO")) {
            ChatResponderReply reply = respond(responder, hello);
            assertTrue(reply.content().startsWith("Xin ch\u00e0o!"));
            assertFalse(reply.content().contains("[HANDOFF]"));
        }
        assertTrue(respond(responder, "gi\u1edd m\u1edf c\u1eeda").content().contains("07:00"));
        assertTrue(respond(responder, "gio mo cua").content().contains("22:00"));
        FaqKnowledgeBase generatedKeywords = new FaqKnowledgeBase("## Court Booking\nCourt booking information.");
        assertEquals("Court booking information.", generatedKeywords.answer("court", 2, 1200).orElseThrow());
    }

    @Test void faqUnknownQuestionOnlyEscalatesAfterRepeatedFallback() {
        ChatAiResponder responder = faqResponder(new ChatAiProperties());
        ChatResponderReply first = respond(responder, "c\u00e2u h\u1ecfi kh\u00f4ng c\u00f3 trong t\u00e0i li\u1ec7u");
        assertTrue(first.content().contains("M\u00ecnh ch\u01b0a c\u00f3 th\u00f4ng tin"), first.content());
        assertTrue(first.content().contains("Gi\u1edd m\u1edf c\u1eeda"), first.content());
        assertTrue(first.metadata().contains("\"fallback\":true"));
        assertFalse(first.content().contains("[HANDOFF]"));

        ChatMessage previousFallback = message(ChatSenderType.AI, first.content());
        previousFallback.setMetadata(first.metadata());
        ChatResponderReply second = responder.respond(new ChatConversation(),
                List.of(previousFallback, message(ChatSenderType.MEMBER, "c\u00e2u h\u1ecfi kh\u00e1c ngo\u00e0i t\u00e0i li\u1ec7u")), 1).orElseThrow();
        assertTrue(second.content().contains("[HANDOFF]"));
        assertTrue(second.metadata().contains("REPEATED_KB_MISS"));
    }

    @Test void faqSensitiveAndExplicitStaffRequestsEscalate() {
        ChatAiResponder responder = faqResponder(new ChatAiProperties());
        ChatResponderReply sensitive = respond(responder, "t\u00f4i mu\u1ed1n ho\u00e0n ti\u1ec1n");
        assertTrue(sensitive.content().contains("B\u1ea1n c\u00f3 mu\u1ed1n chuy\u1ec3n sang nh\u00e2n vi\u00ean kh\u00f4ng?"));
        assertFalse(sensitive.content().contains("M\u00ecnh s\u1ebd chuy\u1ec3n"));
        assertTrue(sensitive.content().contains("[HANDOFF]"));
        assertTrue(respond(responder, "cho t\u00f4i g\u1eb7p nh\u00e2n vi\u00ean").content().contains("[HANDOFF]"));
        assertFalse(respond(responder, "c\u1ea3m \u01a1n").content().contains("[HANDOFF]"));
        assertFalse(respond(responder, "t\u1ea1m bi\u1ec7t").content().contains("[HANDOFF]"));
    }

    @Test void autoModeRetainsOldSensitiveAnswer() {
        ChatAiProperties properties = new ChatAiProperties();
        properties.setHandoffMode("AUTO");
        ChatResponderReply sensitive = respond(faqResponder(properties), "t\u00f4i mu\u1ed1n ho\u00e0n ti\u1ec1n");
        assertTrue(sensitive.content().contains("M\u00ecnh s\u1ebd chuy\u1ec3n"));
    }

    @Test void deepseekProviderFailureUsesFaqFallback() {
        ChatAiProperties properties = new ChatAiProperties();
        AiCompletionClient client = mock(AiCompletionClient.class);
        when(client.complete(anyString(), anyList(), anyInt())).thenThrow(new AiProviderException("http_402"));
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper());
        ChatResponderReply reply = responder.respond(new ChatConversation(), List.of(message(ChatSenderType.MEMBER, "xin chao")), 1).orElseThrow();
        assertTrue(reply.content().startsWith("Xin ch\u00e0o!"));
        assertFalse(reply.content().contains("[HANDOFF]"));
        assertFalse(reply.content().contains("\u00C3") || reply.content().contains("\u00E1\u00BB"));
    }

    @Test void chatPackageHasNoKnownUndiacriticOrMojibakeDisplayLiterals() throws Exception {
        String toiChua = new String(new char[]{'T','o','i',' ','c','h','u','a'});
        String nhanVien = new String(new char[]{'N','h','a','n',' ','v','i','e','n'});
        String mojibakeA = Character.toString((char) 0x00c3);
        String mojibakeViet = new String(new char[]{(char) 0x00e1, (char) 0x00bb});
        try (var paths = Files.walk(Path.of("src/main/java/com/sportscenter/chat"))) {
            for (Path path : paths.filter(item -> item.toString().endsWith(".java")
                    && !item.getFileName().toString().equals("FaqKnowledgeBase.java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                assertFalse(source.contains(toiChua), path.toString());
                assertFalse(source.contains(nhanVien), path.toString());
                assertFalse(source.contains(mojibakeA), path.toString());
                assertFalse(source.contains(mojibakeViet), path.toString());
            }
        }
    }

    private ChatAiResponder faqResponder(ChatAiProperties properties) {
        return new ChatAiResponder(new FaqAiCompletionClient(), properties, new ObjectMapper());
    }

    private ChatResponderReply respond(ChatAiResponder responder, String text) {
        return responder.respond(new ChatConversation(), List.of(message(ChatSenderType.MEMBER, text)), 1).orElseThrow();
    }

    private boolean hasMojibake(String text) {
        return text.indexOf((char) 0x00c3) >= 0
                || text.contains(new String(new char[] {(char) 0x00e1, (char) 0x00bb}));
    }

    private ChatMessage message(ChatSenderType type, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderType(type);
        message.setContent(content);
        return message;
    }
}
