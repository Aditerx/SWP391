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
            if(conversation.getHandlerType()!=ChatHandlerType.AI) return;
            if (chatService.aiDailyLimitReached(event.memberUserId())) {
                chatService.escalateToStaff(event.conversationId(), "Da dat gioi han ho tro tu dong hom nay. Nhan vien se tiep tuc ho tro ban.");
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
                    if (handoff) chatService.escalateToStaff(event.conversationId(), "Toi chua the ho tro day du yeu cau nay. Nhan vien se tiep tuc ho tro ban.");
                }
                else chatService.escalateToStaff(event.conversationId(), "Tro ly AI chua tao duoc cau tra loi. Nhan vien se tiep tuc ho tro ban.");
                return;
            }
            chatService.escalateToStaff(event.conversationId(), "Tro ly AI hien khong kha dung. Nhan vien se tiep tuc ho tro ban.");
        } catch(RuntimeException ex) {
            log.warn("Chat responder failed conversationId={} errorType={}",event.conversationId(),ex.getClass().getSimpleName());
            if (conversation != null && conversation.getHandlerType() == ChatHandlerType.AI) {
                try {
                    chatService.escalateToStaff(event.conversationId(), "The AI assistant is temporarily unavailable. A staff member will help you.");
                } catch (RuntimeException escalationFailure) {
                    log.warn("Chat escalation failed conversationId={} errorType={}",event.conversationId(),escalationFailure.getClass().getSimpleName());
                }
            }
        }
    }
}
