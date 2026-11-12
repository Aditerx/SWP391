package com.sportscenter.chat;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ChatHandoffMetadataTest {
    @Test void responseDerivesHandoffFieldsAndToleratesMissingOrInvalidMetadata() {
        ChatConversation conversation = new ChatConversation(); conversation.setId(3);
        ChatMessage message = new ChatMessage(); message.setConversation(conversation); message.setSenderType(ChatSenderType.AI);
        message.setContent("Gợi ý"); message.setCreatedAt(LocalDateTime.of(2026,11,12,10,0));
        message.setMetadata("{\"handoffSuggested\":true,\"reasonCode\":\"SENSITIVE_TOPIC\"}");
        ChatMessageResponse suggested = ChatMessageResponse.from(message, 1);
        assertTrue(suggested.handoffSuggested());
        assertEquals("SENSITIVE_TOPIC", suggested.reasonCode());
        for (String metadata : new String[]{null, "", "{bad json}", "{\"handoffSuggested\":\"yes\",\"reasonCode\":4}"}) {
            message.setMetadata(metadata);
            ChatMessageResponse ordinary = ChatMessageResponse.from(message, 1);
            assertFalse(ordinary.handoffSuggested());
            assertNull(ordinary.reasonCode());
        }
    }

    @Test void invalidModeFallsBackToSuggest() {
        com.sportscenter.chat.ai.ChatAiProperties properties = new com.sportscenter.chat.ai.ChatAiProperties();
        properties.setHandoffMode("unexpected");
        assertEquals("SUGGEST", properties.getHandoffMode());
        assertTrue(properties.isSuggestHandoff());
    }
}
