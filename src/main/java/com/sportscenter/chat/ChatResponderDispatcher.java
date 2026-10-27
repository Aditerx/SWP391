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
        try {
            ChatConversation conversation=chatService.loadForResponder(event.conversationId());
            if(conversation.getHandlerType()!=ChatHandlerType.AI) return;
            List<ChatMessage> recent=chatService.recentForResponder(event.conversationId(),20);
            for(ChatResponder responder:responders) {
                if(!responder.supports(conversation)) continue;
                Optional<ChatResponderReply> reply=responder.respond(conversation,recent,event.memberUserId());
                reply.ifPresent(value->chatService.saveResponderReply(event.conversationId(),value));
                return;
            }
        } catch(RuntimeException ex) {
            log.warn("Chat responder failed conversationId={}: {}",event.conversationId(),ex.getMessage());
        }
    }
}
