package com.sportscenter.chat;

import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.notification.NotificationType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatMessageEventListenerTest {
    NotificationService notifications=mock(NotificationService.class);
    ChatMessageEventListener listener=new ChatMessageEventListener(notifications);
    @Test void consecutiveMessageEventsOnlyNotifyOnUnreadTransition() {
        listener.onMessage(new ChatMessageCreatedEvent(2,11,ChatSenderType.MEMBER,true,7,List.of(10,12)));
        listener.onMessage(new ChatMessageCreatedEvent(2,12,ChatSenderType.MEMBER,false,7,List.of()));
        verify(notifications,times(1)).create(argThat(e->e.type()==NotificationType.CHAT_MEMBER_MESSAGE&&e.recipientIds().equals(List.of(10,12))));
    }
    @Test void notificationFailureDoesNotEscapeListener() {
        doThrow(new IllegalStateException("notification storage unavailable")).when(notifications).create(any(NotificationEvent.class));
        listener.onMessage(new ChatMessageCreatedEvent(2,11,ChatSenderType.STAFF,true,7,List.of(7)));
        verify(notifications).create(any(NotificationEvent.class));
    }
    @Test void aiReplyNotifiesMemberWhenUnreadTransitions() {
        listener.onMessage(new ChatMessageCreatedEvent(2,11,ChatSenderType.AI,true,7,List.of(7)));
        verify(notifications).create(argThat(e->e.type()==NotificationType.CHAT_STAFF_REPLY&&e.recipientIds().equals(List.of(7))));
    }
}
