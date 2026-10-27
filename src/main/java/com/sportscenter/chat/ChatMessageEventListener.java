package com.sportscenter.chat;

import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.List;

@Component @RequiredArgsConstructor
public class ChatMessageEventListener {
    private static final Logger log=LoggerFactory.getLogger(ChatMessageEventListener.class);
    private final NotificationService notifications;
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onMessage(ChatMessageCreatedEvent event) {
        if(!event.unreadTransition()) return;
        List<Integer> recipients=(event.senderType()==ChatSenderType.MEMBER||event.senderType()==ChatSenderType.SYSTEM)
                ?event.recipientIds():List.of(event.memberUserId());
        if(recipients.isEmpty()) return;
        NotificationType type=(event.senderType()==ChatSenderType.MEMBER||event.senderType()==ChatSenderType.SYSTEM)
                ?NotificationType.CHAT_MEMBER_MESSAGE:NotificationType.CHAT_STAFF_REPLY;
        String title=event.senderType()==ChatSenderType.SYSTEM?"Support conversation escalated":
                type==NotificationType.CHAT_MEMBER_MESSAGE?"New member chat message":"New chat reply";
        String message=event.senderType()==ChatSenderType.SYSTEM?"A support conversation was transferred to staff.":
                type==NotificationType.CHAT_MEMBER_MESSAGE?"A member sent a new support message.":"Staff replied to your support conversation.";
        try {
            notifications.create(new NotificationEvent(recipients,type,title,message,
                    "CHAT_CONVERSATION",event.conversationId(),"CHAT:"+event.conversationId()+":"+type+":"+event.messageId()));
        } catch(RuntimeException ex) {
            log.warn("Unable to create chat notification conversationId={} messageId={}: {}",event.conversationId(),event.messageId(),ex.getMessage());
        }
    }
}
