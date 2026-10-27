package com.sportscenter.chat;

import com.sportscenter.audit.AuditService;
import com.sportscenter.user.Role;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    ChatConversationRepository conversations=mock(ChatConversationRepository.class);
    ChatMessageRepository messages=mock(ChatMessageRepository.class);
    ChatReadStateRepository readStates=mock(ChatReadStateRepository.class);
    UserRepository users=mock(UserRepository.class);
    AuditService audit=mock(AuditService.class);
    ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    ChatService service;
    User member;
    UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("member@example.com","n/a",List.of(new SimpleGrantedAuthority("ROLE_MEMBER"),new SimpleGrantedAuthority("CHAT_AS_MEMBER")));

    @BeforeEach void setup() {
        service=new ChatService(conversations,messages,readStates,users,audit,events,Clock.fixed(Instant.parse("2026-10-24T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        Role role=new Role(); role.setName("Member"); member=new User(); member.setId(7); member.setEmail("member@example.com"); member.setStatus("Active"); member.setRole(role);
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(member));
        when(users.findByIdForUpdate(7)).thenReturn(Optional.of(member));
        when(readStates.findByConversation_IdAndUser_Id(anyInt(),anyInt())).thenReturn(Optional.empty());
        when(messages.latestId(anyInt())).thenReturn(0);
        when(messages.countUnread(anyInt(),anyInt(),anyBoolean())).thenReturn(0L);
    }

    @Test void getOrCreateReturnsExistingOpenConversation() {
        ChatConversation c=conversation(12,member); when(conversations.findFirstByMember_IdAndStatusOrderByIdDesc(7,ChatConversationStatus.OPEN)).thenReturn(Optional.of(c));
        when(messages.findByConversation_IdOrderByIdDesc(eq(12),any())).thenReturn(List.of());
        ChatConversationResponse result=service.getOrCreate(auth,new ChatConversationRequest("ignored"));
        assertEquals(12,result.id()); verify(conversations,never()).saveAndFlush(any());
    }

    @Test void getOrCreateCreatesOpenStaffConversationWithVietnamClock() {
        when(conversations.findFirstByMember_IdAndStatusOrderByIdDesc(7,ChatConversationStatus.OPEN)).thenReturn(Optional.empty());
        when(conversations.saveAndFlush(any())).thenAnswer(inv->{ChatConversation c=inv.getArgument(0); c.setId(15); return c;});
        when(messages.findByConversation_IdOrderByIdDesc(eq(15),any())).thenReturn(List.of());
        ChatConversationResponse result=service.getOrCreate(auth,new ChatConversationRequest("  Billing  "));
        assertEquals("Billing",result.subject()); assertEquals("STAFF",result.handlerType().name());
        assertEquals("2026-10-24T12:00+07:00",result.createdAt().toString());
    }

    @Test void memberCannotReadAnotherMembersConversation() {
        User other=new User(); other.setId(8); ChatConversation c=conversation(3,other);
        when(conversations.findById(3)).thenReturn(Optional.of(c));
        assertEquals("CHAT_CONVERSATION_NOT_FOUND",assertThrows(ChatException.class,()->service.listMessages(3,0,30,auth)).getCode());
    }

    @Test void memberCannotSendToAnotherMembersConversation() {
        User other=new User(); other.setId(8); ChatConversation c=conversation(3,other);
        when(conversations.findByIdForUpdate(3)).thenReturn(Optional.of(c));
        assertEquals("CHAT_CONVERSATION_NOT_FOUND",assertThrows(ChatException.class,()->service.sendMessage(3,new ChatMessageRequest("hello",null),auth)).getCode());
        verify(messages,never()).saveAndFlush(any());
    }

    @Test void absentConversationAndForeignConversationShareNotFoundCode() {
        when(conversations.findById(999)).thenReturn(Optional.empty());
        assertEquals("CHAT_CONVERSATION_NOT_FOUND",assertThrows(ChatException.class,()->service.listMessages(999,0,30,auth)).getCode());
    }

    @Test void blankAndTooLongMessagesAreRejected() {
        ChatConversation c=conversation(2,member); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        assertEquals("CHAT_MESSAGE_EMPTY",assertThrows(ChatException.class,()->service.sendMessage(2,new ChatMessageRequest("  ",null),auth)).getCode());
        assertEquals("CHAT_MESSAGE_TOO_LONG",assertThrows(ChatException.class,()->service.sendMessage(2,new ChatMessageRequest("x".repeat(2001),null),auth)).getCode());
    }

    @Test void closedConversationRejectsMemberMessage() {
        ChatConversation c=conversation(2,member); c.setStatus(ChatConversationStatus.CLOSED); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        assertEquals("CHAT_CONVERSATION_CLOSED",assertThrows(ChatException.class,()->service.sendMessage(2,new ChatMessageRequest("hello",null),auth)).getCode());
    }

    @Test void duplicateClientMessageIdReturnsPersistedMessage() {
        ChatConversation c=conversation(2,member); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        ChatMessage prior=new ChatMessage(); prior.setId(31); prior.setConversation(c); prior.setSender(member); prior.setSenderType(ChatSenderType.MEMBER); prior.setContent("hello");
        when(messages.findByConversation_IdAndSender_IdAndClientMessageId(2,7,"retry-1")).thenReturn(Optional.of(prior));
        assertEquals(31,service.sendMessage(2,new ChatMessageRequest("hello","retry-1"),auth).id());
        verify(messages,never()).saveAndFlush(any());
    }

    @Test void messagePollingUsesAscendingCursorAndMaximumFifty() {
        ChatConversation c=conversation(2,member); when(conversations.findById(2)).thenReturn(Optional.of(c));
        service.listMessages(2,40,500,auth);
        verify(messages).findByConversation_IdAndIdGreaterThanOrderByIdAsc(eq(2),eq(40),argThat(p->p.getPageSize()==50));
    }

    @Test void unreadAndReadStateArePerViewer() {
        ChatConversation c=conversation(2,member); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        when(messages.latestId(2)).thenReturn(41);
        service.markRead(2,auth);
        verify(readStates).save(argThat(state->state.getLastReadMessageId()==41&&state.getUser().getId()==7));
    }

    @Test void rateLimitReturnsRetryableCode() {
        ChatConversation c=conversation(2,member); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        when(messages.countBySender_IdAndCreatedAtAfter(eq(7),any(LocalDateTime.class))).thenReturn(20L);
        assertEquals(429,assertThrows(ChatException.class,()->service.sendMessage(2,new ChatMessageRequest("hello",null),auth)).getStatus());
    }

    @Test void validMemberMessageIsTrimmedAndEventIsPublishedAfterPersistence() {
        ChatConversation c=conversation(2,member); when(conversations.findByIdForUpdate(2)).thenReturn(Optional.of(c));
        when(users.findActiveReceptionists(null)).thenReturn(List.of());
        when(messages.saveAndFlush(any())).thenAnswer(inv->{ChatMessage m=inv.getArgument(0);m.setId(35);return m;});
        ChatMessageResponse response=service.sendMessage(2,new ChatMessageRequest("  hello  ","id-1"),auth);
        assertEquals("hello",response.content()); assertEquals(35,response.id());
        verify(events).publishEvent(argThat((Object e)->e instanceof ChatMessageCreatedEvent event&&event.messageId()==35));
    }

    @Test void claimReleaseCloseAndReopenAreAudited() {
        Role role=new Role(); role.setName("Receptionist"); User staff=new User(); staff.setId(22); staff.setEmail("staff@example.com"); staff.setStatus("Active"); staff.setRole(role);
        var staffAuth=new UsernamePasswordAuthenticationToken("staff@example.com","n/a",List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"),new SimpleGrantedAuthority("HANDLE_CHAT")));
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));
        ChatConversation c=conversation(5,member); when(conversations.findByIdForUpdate(5)).thenReturn(Optional.of(c));
        when(messages.findByConversation_IdOrderByIdDesc(eq(5),any())).thenReturn(List.of());
        assertEquals(22,service.claim(5,staffAuth).assignedStaffUserId());
        service.release(5,staffAuth); service.close(5,staffAuth); service.reopen(5,staffAuth);
        verify(audit,times(4)).log(eq(22),anyString(),eq("CHAT_CONVERSATION"),eq(5),eq("conversationId=5"),any(LocalDateTime.class));
    }

    @Test void escalationSwitchesHandlerWritesSystemMessageAndPublishesEvent() {
        ChatConversation c=conversation(6,member); c.setHandlerType(ChatHandlerType.AI); when(conversations.findByIdForUpdate(6)).thenReturn(Optional.of(c));
        when(users.findActiveReceptionists(null)).thenReturn(List.of());
        when(messages.saveAndFlush(any())).thenAnswer(inv->{ChatMessage m=inv.getArgument(0);m.setId(60);return m;});
        service.escalateToStaff(6,"needs a person");
        assertEquals(ChatHandlerType.STAFF,c.getHandlerType());
        verify(messages).saveAndFlush(argThat(m->m.getSenderType()==ChatSenderType.SYSTEM&&m.getSender()==null));
        verify(events).publishEvent(argThat((Object e)->e instanceof ChatMessageCreatedEvent event&&event.senderType()==ChatSenderType.SYSTEM));
    }

    @Test void staffMustHaveChatPermissionAndCoachIsRejected() {
        Role coachRole=new Role(); coachRole.setName("Coach"); User coach=new User(); coach.setId(9); coach.setEmail("coach@example.com"); coach.setRole(coachRole);
        var coachAuth=new UsernamePasswordAuthenticationToken("coach@example.com","n/a",List.of(new SimpleGrantedAuthority("ROLE_COACH")));
        when(users.findByEmailIgnoreCase("coach@example.com")).thenReturn(Optional.of(coach));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.inbox(null,null,false,PageRequest.of(0,10),coachAuth));
    }

    private ChatConversation conversation(int id,User owner) { ChatConversation c=new ChatConversation(); c.setId(id); c.setMember(owner); c.setStatus(ChatConversationStatus.OPEN); c.setHandlerType(ChatHandlerType.STAFF); return c; }
}
