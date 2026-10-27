package com.sportscenter.chat;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

class ChatResponderDispatcherTest {
    ChatService service=mock(ChatService.class);
    ChatConversation conversation;
    ChatResponderDispatcher dispatcher;

    ChatResponderDispatcherTest() { conversation=new ChatConversation(); conversation.setId(4); conversation.setHandlerType(ChatHandlerType.AI); }

    @Test void firstSupportingResponderReplyIsPersistedAsAi() {
        ChatResponder ignored=mock(ChatResponder.class), selected=mock(ChatResponder.class), later=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        when(ignored.supports(conversation)).thenReturn(false);
        when(selected.supports(conversation)).thenReturn(true);
        when(selected.respond(eq(conversation),anyList(),eq(7))).thenReturn(Optional.of(new ChatResponderReply("hello","{\"model\":\"test\"}")));
        dispatcher=new ChatResponderDispatcher(List.of(ignored,selected,later),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).saveResponderReply(4,new ChatResponderReply("hello","{\"model\":\"test\"}"));
        verifyNoInteractions(later);
    }

    @Test void responderFailureDoesNotEscapeOrBreakMemberMessageHandling() {
        ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(responder.supports(conversation)).thenReturn(true);
        when(responder.respond(eq(conversation),anyList(),eq(7))).thenThrow(new IllegalStateException("responder unavailable"));
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service,never()).saveResponderReply(anyInt(),any());
    }

    @Test void staffHandledConversationDoesNotInvokeResponder() {
        conversation.setHandlerType(ChatHandlerType.STAFF); ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation); dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verifyNoInteractions(responder);
    }
}
