package com.sportscenter.chat;

import com.sportscenter.chat.ai.ChatAiResponder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
public class ChatResponderDispatcher {
    private static final Logger log=LoggerFactory.getLogger(ChatResponderDispatcher.class);
    private static final String INVITATION = "Bạn có thể bấm 'Gặp nhân viên' để được hỗ trợ trực tiếp.";
    private static final String LIMIT_MESSAGE = "Bạn đã dùng hết lượt hỏi trợ lý AI hôm nay. Bạn có thể bấm 'Gặp nhân viên' để được hỗ trợ trực tiếp.";
    private static final String UNAVAILABLE_MESSAGE = "Trợ lý AI tạm thời chưa phản hồi được. Bạn có thể thử lại hoặc bấm 'Gặp nhân viên'.";
    private final List<ChatResponder> responders;
    private final ChatService chatService;
    @Value("${scms.ai.chat.handoff-mode:SUGGEST}")
    private String handoffMode = "SUGGEST";

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async("chatExecutor")
    public void onMemberMessage(ChatMessageCreatedEvent event) {
        if(event.senderType()!=ChatSenderType.MEMBER) return;
        ChatConversation conversation = null;
        try {
            conversation=chatService.loadForResponder(event.conversationId());
            if(conversation.getStatus()!=ChatConversationStatus.OPEN || conversation.getHandlerType()!=ChatHandlerType.AI) return;
            List<ChatMessage> recent=chatService.recentForResponder(event.conversationId(),20);
            String latestMemberText = recent.stream().filter(message -> message.getSenderType()==ChatSenderType.MEMBER)
                    .reduce((first, second) -> second).map(ChatMessage::getContent).orElse("");
            if (ChatAiResponder.isExplicitStaffRequest(latestMemberText)) {
                chatService.saveResponderReply(event.conversationId(), new ChatResponderReply(
                        "Mình sẽ chuyển cuộc trò chuyện cho nhân viên hỗ trợ ngay.",
                        "{\"model\":\"faq\",\"reasonCode\":\"MEMBER_REQUESTED_STAFF\"}"));
                chatService.escalateToStaff(event.conversationId(), "MEMBER_REQUESTED_STAFF");
                return;
            }
            if (chatService.aiDailyLimitReached(event.memberUserId())) {
                fallback(event.conversationId(), "AI_DAILY_LIMIT_REACHED", LIMIT_MESSAGE);
                return;
            }
            for(ChatResponder responder:responders) {
                if(!responder.supports(conversation)) continue;
                Optional<ChatResponderReply> reply=responder.respond(conversation,recent,event.memberUserId());
                if (reply.isEmpty() || reply.get().content()==null || reply.get().content().isBlank()) {
                    fallback(event.conversationId(), "AI_EMPTY_REPLY", UNAVAILABLE_MESSAGE);
                    return;
                }
                ChatResponderReply response = reply.get();
                boolean handoff = response.content().contains("[HANDOFF]");
                String visible = response.content().replace("[HANDOFF]", "").trim();
                if (!handoff) {
                    if (!visible.isBlank()) chatService.saveResponderReply(event.conversationId(), new ChatResponderReply(visible, response.metadata()));
                    return;
                }
                String reason = handoffReason(response.metadata());
                if ("MEMBER_REQUESTED_STAFF".equals(reason)) {
                    chatService.escalateToStaff(event.conversationId(), reason);
                    return;
                }
                if (isAuto()) {
                    if (!visible.isBlank()) chatService.saveResponderReply(event.conversationId(), new ChatResponderReply(visible, response.metadata()));
                    chatService.escalateToStaff(event.conversationId(), reason);
                    return;
                }
                if (visible.isBlank()) {
                    chatService.saveResponderReply(event.conversationId(),
                            new ChatResponderReply(INVITATION, ChatHandoffMetadata.withSuggestion(response.metadata(), reason)));
                } else {
                    if (!hasInvitation(visible)) visible = visible + " " + INVITATION;
                    chatService.saveResponderReply(event.conversationId(),
                            new ChatResponderReply(visible, ChatHandoffMetadata.withSuggestion(response.metadata(), reason)));
                }
                return;
            }
            fallback(event.conversationId(), "AI_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        } catch(RuntimeException ex) {
            log.warn("Chat responder failed conversationId={} errorType={}",event.conversationId(),ex.getClass().getSimpleName());
            if (conversation != null && conversation.getHandlerType() == ChatHandlerType.AI) {
                fallback(event.conversationId(), "AI_RUNTIME_FAILURE", UNAVAILABLE_MESSAGE);
            }
        }
    }

    private void fallback(Integer conversationId, String reason, String message) {
        if (isAuto()) chatService.escalateToStaff(conversationId, reason);
        else chatService.saveHandoffSuggestion(conversationId, message, reason);
    }

    private boolean isAuto() {
        if ("AUTO".equalsIgnoreCase(handoffMode)) return true;
        if (handoffMode == null || handoffMode.isBlank() || "SUGGEST".equalsIgnoreCase(handoffMode)) return false;
        log.warn("Invalid scms.ai.chat.handoff-mode='{}'; using SUGGEST", handoffMode);
        handoffMode = "SUGGEST";
        return false;
    }

    private boolean hasInvitation(String content) {
        String normalized = java.text.Normalizer.normalize(content, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace("đ", "d").replace("Đ", "D").toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("gap nhan vien");
    }

    private String handoffReason(String metadata) {
        String reason = ChatHandoffMetadata.reasonCode(metadata);
        return reason == null ? "AI_HANDOFF_SUGGESTED" : reason;
    }
}
