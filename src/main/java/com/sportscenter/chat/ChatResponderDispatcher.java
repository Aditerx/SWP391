package com.sportscenter.chat;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
public class ChatResponderDispatcher {
    private static final Logger log=LoggerFactory.getLogger(ChatResponderDispatcher.class);
    private final List<ChatResponder> responders;
    private final ChatService chatService;
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async("chatExecutor")
    public void onMemberMessage(ChatMessageCreatedEvent event) {
        if(event.senderType()!=ChatSenderType.MEMBER) return;
        ChatConversation conversation = null;
        try {
            conversation=chatService.loadForResponder(event.conversationId());
            if(conversation.getStatus()!=ChatConversationStatus.OPEN || conversation.getHandlerType()!=ChatHandlerType.AI) return;
            if (chatService.aiDailyLimitReached(event.memberUserId())) {
                chatService.escalateToStaff(event.conversationId(), "AI_DAILY_LIMIT_REACHED");
                return;
            }
            List<ChatMessage> recent=chatService.recentForResponder(event.conversationId(),20);
            for(ChatResponder responder:responders) {
                if(!responder.supports(conversation)) continue;
                Optional<ChatResponderReply> reply=responder.respond(conversation,recent,event.memberUserId());
                if (reply.isPresent()) {
                    ChatResponderReply response = reply.get();
                    boolean handoff = response.content().contains("[HANDOFF]");
                    String visible = response.content().replace("[HANDOFF]", "").trim();
                    if (!visible.isBlank()) chatService.saveResponderReply(event.conversationId(), new ChatResponderReply(visible, response.metadata()));
                    if (handoff) chatService.escalateToStaff(event.conversationId(), handoffReason(response.metadata()));
                }
                else chatService.escalateToStaff(event.conversationId(), "AI_EMPTY_REPLY");
                return;
            }
            chatService.escalateToStaff(event.conversationId(), "AI_UNAVAILABLE");
        } catch(RuntimeException ex) {
            log.warn("Chat responder failed conversationId={} errorType={}",event.conversationId(),ex.getClass().getSimpleName());
            if (conversation != null && conversation.getHandlerType() == ChatHandlerType.AI) {
                try {
                    chatService.escalateToStaff(event.conversationId(), "AI_RUNTIME_FAILURE");
                } catch (RuntimeException escalationFailure) {
                    log.warn("Chat escalation failed conversationId={} errorType={}",event.conversationId(),escalationFailure.getClass().getSimpleName());
                }
            }
        }
    }

    private String handoffReason(String metadata) {
        if (metadata == null) return "AI_HANDOFF_REQUESTED";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"reasonCode\\\":\\\"([A-Z0-9_]{1,40})\\\"").matcher(metadata);
        return matcher.find() ? matcher.group(1) : "AI_HANDOFF_REQUESTED";
    }
}
