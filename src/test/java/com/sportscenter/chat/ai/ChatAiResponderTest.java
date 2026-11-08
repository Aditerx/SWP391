package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.chat.ChatConversation;
import com.sportscenter.chat.ChatMessage;
import com.sportscenter.chat.ChatResponderReply;
import com.sportscenter.chat.ChatSenderType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.nio.charset.StandardCharsets;
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

    @Test void providerFailureFallsBackInVietnameseAndRequestsHandoffWithoutLoggingContent() {
        ChatAiProperties properties = new ChatAiProperties();
        AiCompletionClient client = mock(AiCompletionClient.class);
        when(client.complete(anyString(), anyList(), anyInt())).thenThrow(new AiProviderException("http_402"));
        ChatAiResponder responder = new ChatAiResponder(client, properties, new ObjectMapper());
        ChatResponderReply reply = responder.respond(new ChatConversation(), List.of(message(ChatSenderType.MEMBER, "xin chao")), 1).orElseThrow();
        assertTrue(reply.content().contains("[HANDOFF]"));
        assertTrue(reply.content().contains("nhan vien"));
        assertFalse(reply.content().contains("\u00C3") || reply.content().contains("\u00E1\u00BB"));
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
