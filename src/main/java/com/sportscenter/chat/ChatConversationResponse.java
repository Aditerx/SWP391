package com.sportscenter.chat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
public record ChatConversationResponse(Integer id, Integer memberUserId, String memberName, ChatConversationStatus status,
        Integer assignedStaffUserId, String assignedStaffName, ChatHandlerType handlerType, String subject,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime lastMessageAt, OffsetDateTime closedAt,
        ChatMessageResponse lastMessage, long unreadCount) {
    static ChatConversationResponse from(ChatConversation c, ChatMessage last, Integer viewerId, long unread) {
        return new ChatConversationResponse(c.getId(), c.getMember().getId(), c.getMember().getFullName(), c.getStatus(),
                c.getAssignedStaff() == null ? null : c.getAssignedStaff().getId(),
                c.getAssignedStaff() == null ? null : c.getAssignedStaff().getFullName(), c.getHandlerType(),
                c.getSubject(), offset(c.getCreatedAt()), offset(c.getUpdatedAt()), offset(c.getLastMessageAt()), offset(c.getClosedAt()),
                last == null ? null : ChatMessageResponse.from(last, viewerId), unread);
    }
    private static OffsetDateTime offset(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime(); }
}
