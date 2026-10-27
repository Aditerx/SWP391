package com.sportscenter.chat;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.notification.NotificationType;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ChatReadStateRepository readStates;
    private final UserRepository users;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ChatConversationResponse getOrCreate(Authentication auth, ChatConversationRequest request) {
        User authenticatedMember = requireMember(auth);
        User member = users.findByIdForUpdate(authenticatedMember.getId()).orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        ChatConversation conversation = conversations.findFirstByMember_IdAndStatusOrderByIdDesc(member.getId(), ChatConversationStatus.OPEN)
                .orElseGet(() -> {
                    LocalDateTime now = now(); ChatConversation c = new ChatConversation(); c.setMember(member);
                    c.setStatus(ChatConversationStatus.OPEN); c.setHandlerType(ChatHandlerType.STAFF);
                    c.setSubject(cleanSubject(request == null ? null : request.subject())); c.setCreatedAt(now); c.setUpdatedAt(now);
                    return conversations.saveAndFlush(c);
                });
        return response(conversation, member.getId());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse active(Authentication auth) {
        User member = requireMember(auth);
        return conversations.findFirstByMember_IdAndStatusOrderByIdDesc(member.getId(), ChatConversationStatus.OPEN)
                .map(c -> response(c, member.getId())).orElseThrow(() -> new ChatException(404,"CHAT_ACTIVE_CONVERSATION_NOT_FOUND","No open chat conversation"));
    }

    @Transactional(readOnly = true)
    public ChatUnreadCountResponse memberUnread(Authentication auth) {
        User member = requireMember(auth);
        long total=conversations.findByMember_Id(member.getId()).stream().mapToLong(c->unread(c,member.getId(),false)).sum();
        return new ChatUnreadCountResponse(total);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Integer conversationId, Integer afterId, Integer requestedLimit, Authentication auth) {
        User viewer = currentUser(auth); ChatConversation c = accessible(conversationId, viewer, auth);
        int limit = requestedLimit == null ? 30 : Math.max(1, Math.min(50, requestedLimit));
        int cursor = afterId == null ? 0 : Math.max(0, afterId);
        return messages.findByConversation_IdAndIdGreaterThanOrderByIdAsc(c.getId(), cursor, PageRequest.of(0, limit))
                .stream().map(m -> ChatMessageResponse.from(m, viewer.getId())).toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Integer conversationId, ChatMessageRequest request, Authentication auth) {
        User sender = currentUser(auth); ChatConversation c = conversations.findByIdForUpdate(conversationId).orElseThrow(ChatService::notFound);
        boolean member = isMember(sender);
        if (member && !Objects.equals(c.getMember().getId(), sender.getId())) throw notFound();
        if (member && !"Active".equalsIgnoreCase(sender.getStatus())) throw new org.springframework.security.access.AccessDeniedException("Active member account required");
        if (!member && (!isStaff(sender) || !hasPermission(auth,"HANDLE_CHAT"))) throw new org.springframework.security.access.AccessDeniedException("Chat access denied");
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) throw new ChatException(400,"CHAT_MESSAGE_EMPTY","Message content must not be empty");
        if (content.length() > 2000) throw new ChatException(400,"CHAT_MESSAGE_TOO_LONG","Message content must not exceed 2000 characters");
        if (member && c.getStatus() == ChatConversationStatus.CLOSED) throw new ChatException(409,"CHAT_CONVERSATION_CLOSED","Conversation is closed");
        String clientId = request.clientMessageId();
        if (clientId != null && !clientId.isBlank()) {
            Optional<ChatMessage> prior = messages.findByConversation_IdAndSender_IdAndClientMessageId(c.getId(), sender.getId(), clientId.trim());
            if (prior.isPresent()) return ChatMessageResponse.from(prior.get(), sender.getId());
        }
        LocalDateTime now = now();
        long count = messages.countBySender_IdAndCreatedAtAfter(sender.getId(), now.minusMinutes(1));
        if (count >= 20) {
            LocalDateTime oldest=messages.findFirstBySender_IdAndCreatedAtAfterOrderByCreatedAtAsc(sender.getId(),now.minusMinutes(1))
                    .map(ChatMessage::getCreatedAt).orElse(now.minusMinutes(1));
            int seconds=Math.max(1,60-(int)java.time.Duration.between(oldest,now).getSeconds());
            throw new ChatException(429,"CHAT_RATE_LIMITED","Please wait before sending another message",seconds);
        }
        boolean forStaff = member;
        List<Integer> recipients = member ? staffRecipients(c) : List.of(c.getMember().getId());
        List<Integer> notificationRecipients = member
                ? recipients.stream().filter(id -> unread(c,id,true)==0).toList()
                : (unread(c,c.getMember().getId(),false)==0?recipients:List.of());
        ChatMessage message = new ChatMessage(); message.setConversation(c); message.setSender(sender);
        message.setSenderType(member ? ChatSenderType.MEMBER : ChatSenderType.STAFF); message.setContent(content);
        message.setContentType("TEXT"); message.setClientMessageId(clientId == null || clientId.isBlank() ? null : clientId.trim());
        message.setCreatedAt(now); message = messages.saveAndFlush(message);
        if (!member && c.getAssignedStaff() == null) c.setAssignedStaff(sender);
        c.setLastMessageAt(now); c.setUpdatedAt(now);
        events.publishEvent(new ChatMessageCreatedEvent(c.getId(), message.getId(), message.getSenderType(), !notificationRecipients.isEmpty(),
                c.getMember().getId(), notificationRecipients));
        return ChatMessageResponse.from(message, sender.getId());
    }

    @Transactional
    public void markRead(Integer conversationId, Authentication auth) {
        User viewer = currentUser(auth); ChatConversation c = conversations.findByIdForUpdate(conversationId).orElseThrow(ChatService::notFound);
        if (isMember(viewer)) { if (!c.getMember().getId().equals(viewer.getId())) throw notFound(); if(!"Active".equalsIgnoreCase(viewer.getStatus())) throw new org.springframework.security.access.AccessDeniedException("Active member account required"); }
        else if (!isStaff(viewer) || !hasPermission(auth,"HANDLE_CHAT")) throw new org.springframework.security.access.AccessDeniedException("Chat access denied");
        Integer latest = messages.latestId(c.getId()); ChatReadState state = readStates.findByConversation_IdAndUser_Id(c.getId(),viewer.getId())
                .orElseGet(() -> new ChatReadState(c,viewer));
        state.setLastReadMessageId(latest); state.setUpdatedAt(now()); readStates.save(state);
    }

    @Transactional(readOnly = true)
    public Page<ChatConversationResponse> inbox(ChatConversationStatus status, Boolean assigned, boolean unreadOnly, Pageable requested, Authentication auth) {
        User staff = requireStaff(auth); int size = Math.min(100, Math.max(1, requested.getPageSize()));
        Page<ChatConversation> page = conversations.inbox(status,assigned,staff.getId(),unreadOnly,PageRequest.of(Math.max(0,requested.getPageNumber()),size,Sort.unsorted()));
        return page.map(c -> response(c,staff.getId()));
    }

    @Transactional
    public ChatConversationResponse claim(Integer id, Authentication auth) { return assign(id, auth, true); }
    @Transactional
    public ChatConversationResponse release(Integer id, Authentication auth) { return assign(id, auth, false); }
    private ChatConversationResponse assign(Integer id, Authentication auth, boolean claim) {
        User actor=requireStaff(auth); ChatConversation c=conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        if (claim) {
            if (c.getAssignedStaff()!=null && !c.getAssignedStaff().getId().equals(actor.getId())) throw new ChatException(409,"CHAT_ALREADY_CLAIMED","Conversation is assigned to another staff member");
            c.setAssignedStaff(actor); audit.log(actor.getId(),"CHAT_CLAIM","CHAT_CONVERSATION",id,"conversationId="+id,now());
        } else {
            if (c.getAssignedStaff()==null || !c.getAssignedStaff().getId().equals(actor.getId())) throw new ChatException(409,"CHAT_NOT_ASSIGNED_TO_ACTOR","Only the assigned staff member can release this conversation");
            c.setAssignedStaff(null); audit.log(actor.getId(),"CHAT_RELEASE","CHAT_CONVERSATION",id,"conversationId="+id,now());
        }
        c.setUpdatedAt(now()); return response(c,actor.getId());
    }

    @Transactional
    public ChatConversationResponse close(Integer id, Authentication auth) { return setStatus(id,auth,true); }
    @Transactional
    public ChatConversationResponse reopen(Integer id, Authentication auth) { return setStatus(id,auth,false); }
    private ChatConversationResponse setStatus(Integer id, Authentication auth, boolean close) {
        User actor=requireStaff(auth); ChatConversation c=conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        if (!close) {
            ChatConversation open = conversations.findFirstByMember_IdAndStatusOrderByIdDesc(c.getMember().getId(),ChatConversationStatus.OPEN).orElse(null);
            if (open != null && !open.getId().equals(c.getId())) throw new ChatException(409,"CHAT_OPEN_CONVERSATION_EXISTS","Member already has another open conversation");
        }
        c.setStatus(close?ChatConversationStatus.CLOSED:ChatConversationStatus.OPEN); c.setClosedAt(close?now():null); c.setUpdatedAt(now());
        audit.log(actor.getId(),close?"CHAT_CLOSE":"CHAT_REOPEN","CHAT_CONVERSATION",id,"conversationId="+id,now());
        return response(c,actor.getId());
    }

    @Transactional
    public void escalateToStaff(Integer id, String reason) {
        ChatConversation c=conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        List<Integer> targets=staffRecipients(c).stream().filter(target->unread(c,target,true)==0).toList();
        audit.log(null,"CHAT_ESCALATE","CHAT_CONVERSATION",id,"conversationId="+id,now());
        c.setHandlerType(ChatHandlerType.STAFF); String safeReason=reason==null?"":reason.trim();
        ChatMessage m=new ChatMessage(); m.setConversation(c); m.setSenderType(ChatSenderType.SYSTEM); m.setContent(safeReason.isEmpty()?"This conversation has been transferred to our staff.":"Transferred to staff: "+safeReason);
        m.setContentType("TEXT"); m.setCreatedAt(now()); messages.saveAndFlush(m); c.setLastMessageAt(m.getCreatedAt()); c.setUpdatedAt(m.getCreatedAt());
        events.publishEvent(new ChatMessageCreatedEvent(c.getId(),m.getId(),ChatSenderType.SYSTEM,!targets.isEmpty(),c.getMember().getId(),targets));
    }

    @Transactional
    public void saveResponderReply(Integer conversationId, ChatResponderReply reply) {
        if (reply==null || reply.content()==null || reply.content().isBlank()) return;
        ChatConversation c=conversations.findByIdForUpdate(conversationId).orElseThrow(ChatService::notFound);
        if (c.getHandlerType()!=ChatHandlerType.AI || c.getStatus()!=ChatConversationStatus.OPEN) return;
        boolean transition=unread(c,c.getMember().getId(),false)==0;
        LocalDateTime now=now(); ChatMessage m=new ChatMessage(); m.setConversation(c); m.setSenderType(ChatSenderType.AI); m.setContent(reply.content().trim());
        m.setMetadata(reply.metadata()); m.setContentType("TEXT"); m.setCreatedAt(now); messages.saveAndFlush(m); c.setLastMessageAt(now); c.setUpdatedAt(now);
        events.publishEvent(new ChatMessageCreatedEvent(c.getId(),m.getId(),ChatSenderType.AI,transition,c.getMember().getId(),List.of(c.getMember().getId())));
    }

    @Transactional(readOnly=true)
    public ChatConversation loadForResponder(Integer id) { return conversations.findById(id).orElseThrow(ChatService::notFound); }
    @Transactional(readOnly=true)
    public List<ChatMessage> recentForResponder(Integer id, int limit) { List<ChatMessage> rows=messages.latestMessages(id,PageRequest.of(0,Math.max(1,Math.min(50,limit)))); Collections.reverse(rows); return rows; }

    private ChatConversationResponse response(ChatConversation c,Integer viewerId) {
        List<ChatMessage> last=messages.findByConversation_IdOrderByIdDesc(c.getId(),PageRequest.of(0,1));
        boolean staff=viewerId!=null && !c.getMember().getId().equals(viewerId);
        return ChatConversationResponse.from(c,last.isEmpty()?null:last.get(0),viewerId,unread(c,viewerId,staff));
    }
    private long unread(ChatConversation c,Integer userId,boolean staff) {
        int after=readStates.findByConversation_IdAndUser_Id(c.getId(),userId).map(s->s.getLastReadMessageId()==null?0:s.getLastReadMessageId()).orElse(0);
        return messages.countUnread(c.getId(),after,staff);
    }
    private List<Integer> staffRecipients(ChatConversation c) {
        if(c.getAssignedStaff()!=null) return List.of(c.getAssignedStaff().getId());
        return users.findActiveReceptionists(c.getMember().getCenterId()).stream().map(User::getId).toList();
    }
    private ChatConversation accessible(Integer id,User user,Authentication auth) {
        ChatConversation c=conversations.findById(id).orElseThrow(ChatService::notFound);
        if(isMember(user) && !c.getMember().getId().equals(user.getId())) throw notFound();
        if(isMember(user) && !"Active".equalsIgnoreCase(user.getStatus())) throw new org.springframework.security.access.AccessDeniedException("Active member account required");
        if(!isMember(user) && (!isStaff(user) || !hasPermission(auth,"HANDLE_CHAT"))) throw new org.springframework.security.access.AccessDeniedException("Chat access denied");
        return c;
    }
    private User currentUser(Authentication auth) {
        if(auth==null || !auth.isAuthenticated()) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new ResourceNotFoundException("Authenticated user not found"));
    }
    private User requireMember(Authentication auth) { User u=currentUser(auth); if(!isMember(u)||!"Active".equalsIgnoreCase(u.getStatus())) throw new org.springframework.security.access.AccessDeniedException("Member chat access denied"); return u; }
    private User requireStaff(Authentication auth) { User u=currentUser(auth); if(!isStaff(u)||!hasPermission(auth,"HANDLE_CHAT")) throw new org.springframework.security.access.AccessDeniedException("Staff chat permission required"); return u; }
    private boolean isMember(User u) { return u.getRole()!=null && "MEMBER".equalsIgnoreCase(u.getRole().getName()); }
    private boolean isStaff(User u) { return "Active".equalsIgnoreCase(u.getStatus()) && u.getRole()!=null && ("RECEPTIONIST".equalsIgnoreCase(u.getRole().getName())||"CENTERMANAGER".equalsIgnoreCase(u.getRole().getName())); }
    private boolean hasPermission(Authentication a,String permission) { return a!=null&&a.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(permission::equals); }
    private String cleanSubject(String s) { if(s==null||s.isBlank())return null; String v=s.trim(); return v.length()>200?v.substring(0,200):v; }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private static ChatException notFound() { return new ChatException(404,"CHAT_CONVERSATION_NOT_FOUND","Conversation not found"); }
}
