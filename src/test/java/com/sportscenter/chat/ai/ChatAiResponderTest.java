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

class ChatAiResponderTest {
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
        assertTrue(respond(responder, "t\u00f4i mu\u1ed1n ho\u00e0n ti\u1ec1n").content().contains("[HANDOFF]"));
        assertTrue(respond(responder, "cho t\u00f4i g\u1eb7p nh\u00e2n vi\u00ean").content().contains("[HANDOFF]"));
        assertFalse(respond(responder, "c\u1ea3m \u01a1n").content().contains("[HANDOFF]"));
        assertFalse(respond(responder, "t\u1ea1m bi\u1ec7t").content().contains("[HANDOFF]"));
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
