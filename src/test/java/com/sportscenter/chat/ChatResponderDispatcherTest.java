package com.sportscenter.chat;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

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
        verify(service).saveHandoffSuggestion(4, "Trợ lý AI tạm thời chưa phản hồi được. Bạn có thể thử lại hoặc bấm 'Gặp nhân viên'.", "AI_RUNTIME_FAILURE");
    }

    @Test void staffHandledConversationDoesNotInvokeResponder() {
        conversation.setHandlerType(ChatHandlerType.STAFF); ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation); dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verifyNoInteractions(responder);
    }

    @Test void closedConversationDoesNotInvokeResponderOrEscalate() {
        conversation.setStatus(ChatConversationStatus.CLOSED); ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation); dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verifyNoInteractions(responder);
        verify(service,never()).escalateToStaff(anyInt(),anyString());
    }

    @Test void handoffUsesReasonCodeFromAiMetadata() {
        ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        when(responder.supports(conversation)).thenReturn(true);
        when(responder.respond(eq(conversation),anyList(),eq(7))).thenReturn(Optional.of(
                new ChatResponderReply("Ai transfer. [HANDOFF]", "{\"reasonCode\":\"SENSITIVE_TOPIC\"}")));
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).saveResponderReply(eq(4), argThat(reply ->
                reply.content().contains("Gặp nhân viên")
                        && !reply.content().contains("[HANDOFF]")
                        && ChatHandoffMetadata.suggested(reply.metadata())
                        && "SENSITIVE_TOPIC".equals(ChatHandoffMetadata.reasonCode(reply.metadata()))));
        verify(service,never()).escalateToStaff(anyInt(),anyString());
    }

    @Test void handoffTokenWithoutTextBecomesOnlyInvitation() {
        ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        when(responder.supports(conversation)).thenReturn(true);
        when(responder.respond(eq(conversation),anyList(),eq(7))).thenReturn(Optional.of(new ChatResponderReply("[HANDOFF]", "{}")));
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).saveResponderReply(eq(4),argThat(reply -> reply.content().equals("Bạn có thể bấm 'Gặp nhân viên' để được hỗ trợ trực tiếp.")
                && ChatHandoffMetadata.suggested(reply.metadata())
                && "AI_HANDOFF_SUGGESTED".equals(ChatHandoffMetadata.reasonCode(reply.metadata()))));
    }

    @Test void quotaSavesSystemSuggestionWithoutCallingProvider() {
        ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        when(service.aiDailyLimitReached(7)).thenReturn(true);
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verifyNoInteractions(responder);
        verify(service).saveHandoffSuggestion(4, "Bạn đã dùng hết lượt hỏi trợ lý AI hôm nay. Bạn có thể bấm 'Gặp nhân viên' để được hỗ trợ trực tiếp.", "AI_DAILY_LIMIT_REACHED");
    }

    @Test void noResponderAndEmptyReplySaveOnlySystemSuggestions() {
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        ChatResponder unsupported=mock(ChatResponder.class);
        when(unsupported.supports(conversation)).thenReturn(false);
        dispatcher=new ChatResponderDispatcher(List.of(unsupported),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).saveHandoffSuggestion(4, "Trợ lý AI tạm thời chưa phản hồi được. Bạn có thể thử lại hoặc bấm 'Gặp nhân viên'.", "AI_UNAVAILABLE");

        reset(service);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        ChatResponder empty=mock(ChatResponder.class);
        when(empty.supports(conversation)).thenReturn(true);
        when(empty.respond(eq(conversation),anyList(),eq(7))).thenReturn(Optional.empty());
        dispatcher=new ChatResponderDispatcher(List.of(empty),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).saveHandoffSuggestion(4, "Trợ lý AI tạm thời chưa phản hồi được. Bạn có thể thử lại hoặc bấm 'Gặp nhân viên'.", "AI_EMPTY_REPLY");
        verify(service,never()).escalateToStaff(anyInt(),anyString());
    }

    @Test void explicitMemberRequestTransfersImmediatelyInSuggestMode() {
        ChatResponder responder=mock(ChatResponder.class);
        ChatMessage memberMessage=new ChatMessage(); memberMessage.setSenderType(ChatSenderType.MEMBER); memberMessage.setContent("Cho tôi gặp nhân viên");
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of(memberMessage));
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).escalateToStaff(4,"MEMBER_REQUESTED_STAFF");
        verify(service).saveResponderReply(eq(4),argThat(reply -> reply.content().equals("Mình sẽ chuyển cuộc trò chuyện cho nhân viên hỗ trợ ngay.")));
        verifyNoInteractions(responder);
    }

    @Test void autoModeKeepsAutomaticHandoff() {
        ChatResponder responder=mock(ChatResponder.class);
        when(service.loadForResponder(4)).thenReturn(conversation);
        when(service.recentForResponder(4,20)).thenReturn(List.of());
        when(responder.supports(conversation)).thenReturn(true);
        when(responder.respond(eq(conversation),anyList(),eq(7))).thenReturn(Optional.of(
                new ChatResponderReply("Transfer now. [HANDOFF]", "{\"reasonCode\":\"SENSITIVE_TOPIC\"}")));
        dispatcher=new ChatResponderDispatcher(List.of(responder),service);
        ReflectionTestUtils.setField(dispatcher,"handoffMode","AUTO");
        dispatcher.onMemberMessage(new ChatMessageCreatedEvent(4,5,ChatSenderType.MEMBER,false,7,List.of()));
        verify(service).escalateToStaff(4,"SENSITIVE_TOPIC");
        verify(service).saveResponderReply(4,new ChatResponderReply("Transfer now.","{\"reasonCode\":\"SENSITIVE_TOPIC\"}"));
    }
}
