package com.sportscenter.chat;
import java.util.List;
import java.util.Optional;
public interface ChatResponder {
    boolean supports(ChatConversation conversation);
    Optional<ChatResponderReply> respond(ChatConversation conversation, List<ChatMessage> recentMessages, Integer memberUserId);
}
