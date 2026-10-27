package com.sportscenter.chat;
public record ChatMessageCreatedEvent(Integer conversationId, Integer messageId, ChatSenderType senderType,
                                      boolean unreadTransition, Integer memberUserId, java.util.List<Integer> recipientIds) {
    public ChatMessageCreatedEvent { recipientIds = recipientIds == null ? java.util.List.of() : recipientIds.stream().distinct().toList(); }
}
