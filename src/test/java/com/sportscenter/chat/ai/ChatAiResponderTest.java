package com.sportscenter.chat.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.chat.ChatConversation;
import com.sportscenter.chat.ChatMessage;
import com.sportscenter.chat.ChatResponderReply;
import com.sportscenter.chat.ChatSenderType;
import org.junit.jupiter.api.Test;
import java.util.List;
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

        verify(client).complete(eq(properties.getSystemPrompt()), argThat(messages -> messages.size() == 1
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

    private ChatMessage message(ChatSenderType type, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderType(type);
        message.setContent(content);
        return message;
    }
}
