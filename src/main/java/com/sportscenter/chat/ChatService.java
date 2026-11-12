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
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${scms.ai.chat.new-conversation-handler:STAFF}")
    private ChatHandlerType newConversationHandler = ChatHandlerType.STAFF;
    @Value("${scms.ai.chat.enabled:false}")
    private boolean aiChatEnabled;
    @Value("${scms.ai.chat.daily-limit-per-member:30}")
    private int aiDailyLimitPerMember = 30;

    @Transactional
    public ChatConversationResponse getOrCreate(Authentication auth, ChatConversationRequest request) {
        User authenticatedMember = requireMember(auth);
        User member = users.findByIdForUpdate(authenticatedMember.getId()).orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        ChatConversation conversation = conversations.findFirstByMember_IdAndStatusOrderByIdDesc(member.getId(), ChatConversationStatus.OPEN)
                .orElseGet(() -> {
                    LocalDateTime now = now(); ChatConversation c = new ChatConversation(); c.setMember(member);
                    c.setStatus(ChatConversationStatus.OPEN);
                    c.setHandlerType(aiChatEnabled ? newConversationHandler : ChatHandlerType.STAFF);
                    c.setSubject(cleanSubject(request == null ? null : request.subject())); c.setCreatedAt(now); c.setUpdatedAt(now);
                    return conversations.saveAndFlush(c);
                });
        return response(conversation, member.getId());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse active(Authentication auth) {
        User member = requireMember(auth);
        return conversations.findFirstByMember_IdAndStatusOrderByIdDesc(member.getId(), ChatConversationStatus.OPEN)
                .map(c -> response(c, member.getId())).orElseThrow(() -> new ChatException(404,"CHAT_ACTIVE_CONVERSATION_NOT_FOUND","Kh\u00f4ng t\u00ecm th\u1ea5y cu\u1ed9c tr\u00f2 chuy\u1ec7n \u0111ang m\u1edf"));
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
        if (member && !"Active".equalsIgnoreCase(sender.getStatus())) throw new org.springframework.security.access.AccessDeniedException("T\u00e0i kho\u1ea3n th\u00e0nh vi\u00ean ch\u01b0a ho\u1ea1t \u0111\u1ed9ng");
        if (!member && (!isStaff(sender) || !hasPermission(auth,"HANDLE_CHAT"))) throw new org.springframework.security.access.AccessDeniedException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp cu\u1ed9c tr\u00f2 chuy\u1ec7n");
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) throw new ChatException(400,"CHAT_MESSAGE_EMPTY","N\u1ed9i dung tin nh\u1eafn kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng");
        if (content.length() > 2000) throw new ChatException(400,"CHAT_MESSAGE_TOO_LONG","Tin nh\u1eafn kh\u00f4ng \u0111\u01b0\u1ee3c d\u00e0i qu\u00e1 2.000 k\u00fd t\u1ef1");
        if (member && c.getStatus() == ChatConversationStatus.CLOSED) throw new ChatException(409,"CHAT_CONVERSATION_CLOSED","Cu\u1ed9c tr\u00f2 chuy\u1ec7n \u0111\u00e3 \u0111\u00f3ng");
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
            throw new ChatException(429,"CHAT_RATE_LIMITED","Vui l\u00f2ng \u0111\u1ee3i tr\u01b0\u1edbc khi g\u1eedi tin nh\u1eafn ti\u1ebfp theo",seconds);
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
        if (isMember(viewer)) { if (!c.getMember().getId().equals(viewer.getId())) throw notFound(); if(!"Active".equalsIgnoreCase(viewer.getStatus())) throw new org.springframework.security.access.AccessDeniedException("T\u00e0i kho\u1ea3n th\u00e0nh vi\u00ean ch\u01b0a ho\u1ea1t \u0111\u1ed9ng"); }
        else if (!isStaff(viewer) || !hasPermission(auth,"HANDLE_CHAT")) throw new org.springframework.security.access.AccessDeniedException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp cu\u1ed9c tr\u00f2 chuy\u1ec7n");
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
            if (c.getAssignedStaff()!=null && !c.getAssignedStaff().getId().equals(actor.getId())) throw new ChatException(409,"CHAT_ALREADY_CLAIMED","Cu\u1ed9c tr\u00f2 chuy\u1ec7n \u0111\u00e3 \u0111\u01b0\u1ee3c nh\u00e2n vi\u00ean kh\u00e1c ti\u1ebfp nh\u1eadn");
            c.setAssignedStaff(actor); audit.log(actor.getId(),"CHAT_CLAIM","CHAT_CONVERSATION",id,"conversationId="+id,now());
        } else {
            if (c.getAssignedStaff()==null || !c.getAssignedStaff().getId().equals(actor.getId())) throw new ChatException(409,"CHAT_NOT_ASSIGNED_TO_ACTOR","Ch\u1ec9 nh\u00e2n vi\u00ean \u0111\u00e3 ti\u1ebfp nh\u1eadn m\u1edbi c\u00f3 th\u1ec3 nh\u1ea3 cu\u1ed9c tr\u00f2 chuy\u1ec7n n\u00e0y");
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
            if (open != null && !open.getId().equals(c.getId())) throw new ChatException(409,"CHAT_OPEN_CONVERSATION_EXISTS","B\u1ea1n \u0111ang c\u00f3 m\u1ed9t cu\u1ed9c tr\u00f2 chuy\u1ec7n kh\u00e1c ch\u01b0a \u0111\u00f3ng");
        }
        if (close) closeConversation(c, now());
        else { c.setStatus(ChatConversationStatus.OPEN); c.setClosedAt(null); c.setUpdatedAt(now()); }
        audit.log(actor.getId(),close?"CHAT_CLOSE":"CHAT_REOPEN","CHAT_CONVERSATION",id,"conversationId="+id,now());
        return response(c,actor.getId());
    }

    @Transactional
    public void escalateToStaff(Integer id, String reason) {
        ChatConversation c=conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        if (c.getStatus() != ChatConversationStatus.OPEN) return;
         if (c.getHandlerType() == ChatHandlerType.STAFF) return;
        List<Integer> targets=staffRecipients(c).stream().filter(target->unread(c,target,true)==0).toList();
        audit.log(null,"CHAT_ESCALATE","CHAT_CONVERSATION",id,"conversationId="+id,now());
        c.setHandlerType(ChatHandlerType.STAFF);
        String reasonCode = reason == null || reason.isBlank() ? "STAFF_REQUESTED" : reason.trim().replaceAll("[^A-Za-z0-9_:-]", "_");
        ChatMessage m=new ChatMessage(); m.setConversation(c); m.setSenderType(ChatSenderType.SYSTEM);
        m.setContent("M\u00ecnh \u0111\u00e3 chuy\u1ec3n cu\u1ed9c tr\u00f2 chuy\u1ec7n cho nh\u00e2n vi\u00ean h\u1ed7 tr\u1ee3. Nh\u00e2n vi\u00ean s\u1ebd ph\u1ea3n h\u1ed3i b\u1ea1n s\u1edbm nh\u1ea5t c\u00f3 th\u1ec3.");
        m.setMetadata("{\"reasonCode\":\"" + reasonCode + "\"}");
        m.setContentType("TEXT"); m.setCreatedAt(now()); messages.saveAndFlush(m); c.setLastMessageAt(m.getCreatedAt()); c.setUpdatedAt(m.getCreatedAt());
        events.publishEvent(new ChatMessageCreatedEvent(c.getId(),m.getId(),ChatSenderType.SYSTEM,!targets.isEmpty(),c.getMember().getId(),targets));
    }

    @Transactional
    public ChatConversationResponse requestStaff(Integer id, Authentication auth) {
        User member = requireMember(auth);
        ChatConversation conversation = conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        if (!Objects.equals(conversation.getMember().getId(), member.getId())) throw notFound();
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            throw new ChatException(409, "CHAT_CONVERSATION_CLOSED", "Cu\u1ed9c tr\u00f2 chuy\u1ec7n \u0111\u00e3 \u0111\u00f3ng");
        }
        escalateToStaff(id, "MEMBER_REQUESTED_STAFF");
        return response(conversation, member.getId());
    }

    @Transactional
    public ChatConversationResponse closeAsMember(Integer id, Authentication auth) {
        User member = requireMember(auth);
        ChatConversation conversation = conversations.findByIdForUpdate(id).orElseThrow(ChatService::notFound);
        if (!Objects.equals(conversation.getMember().getId(), member.getId())) throw notFound();
        if (conversation.getStatus() == ChatConversationStatus.OPEN) {
            LocalDateTime closedAt = now();
            closeConversation(conversation, closedAt);
            conversation.setLastMessageAt(closedAt);
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setConversation(conversation);
            systemMessage.setSenderType(ChatSenderType.SYSTEM);
            systemMessage.setContent("B\u1ea1n \u0111\u00e3 k\u1ebft th\u00fac cu\u1ed9c tr\u00f2 chuy\u1ec7n.");
            systemMessage.setMetadata("{\"closedBy\":\"MEMBER\"}");
            systemMessage.setContentType("TEXT");
            systemMessage.setCreatedAt(closedAt);
            messages.saveAndFlush(systemMessage);
        }
        return response(conversation, member.getId());
    }

    @Transactional
    public void saveResponderReply(Integer conversationId, ChatResponderReply reply) {
        if (reply==null || reply.content()==null || reply.content().isBlank()) return;
        ChatConversation c=conversations.findByIdForUpdate(conversationId).orElseThrow(ChatService::notFound);
        if (c.getStatus()!=ChatConversationStatus.OPEN) {
            log.debug("Skipping AI reply for closed chat conversationId={}", conversationId);
            return;
        }
        if (c.getHandlerType()!=ChatHandlerType.AI) return;
        boolean transition=unread(c,c.getMember().getId(),false)==0;
        LocalDateTime now=now(); ChatMessage m=new ChatMessage(); m.setConversation(c); m.setSenderType(ChatSenderType.AI); m.setContent(reply.content().trim());
        m.setMetadata(reply.metadata()); m.setContentType("TEXT"); m.setCreatedAt(now); messages.saveAndFlush(m); c.setLastMessageAt(now); c.setUpdatedAt(now);
        events.publishEvent(new ChatMessageCreatedEvent(c.getId(),m.getId(),ChatSenderType.AI,transition,c.getMember().getId(),List.of(c.getMember().getId())));
    }

    @Transactional
    public void saveHandoffSuggestion(Integer conversationId, String content, String reasonCode) {
        ChatConversation c=conversations.findByIdForUpdate(conversationId).orElseThrow(ChatService::notFound);
        if (c.getStatus()!=ChatConversationStatus.OPEN || c.getHandlerType()!=ChatHandlerType.AI) return;
        ChatMessage lastSystem = messages.findFirstByConversation_IdAndSenderTypeOrderByIdDesc(conversationId, ChatSenderType.SYSTEM).orElse(null);
        if (lastSystem != null && ChatHandoffMetadata.suggested(lastSystem.getMetadata())
                && Objects.equals(reasonCode, ChatHandoffMetadata.reasonCode(lastSystem.getMetadata()))) return;
        LocalDateTime timestamp=now();
        ChatMessage message=new ChatMessage(); message.setConversation(c); message.setSenderType(ChatSenderType.SYSTEM);
        message.setContent(content); message.setContentType("TEXT");
        message.setMetadata(ChatHandoffMetadata.withSuggestion("{}", reasonCode)); message.setCreatedAt(timestamp);
        messages.saveAndFlush(message); c.setLastMessageAt(timestamp); c.setUpdatedAt(timestamp);
    }

    @Transactional(readOnly=true)
    public ChatConversation loadForResponder(Integer id) { return conversations.findById(id).orElseThrow(ChatService::notFound); }
    @Transactional(readOnly=true)
    public List<ChatMessage> recentForResponder(Integer id, int limit) { List<ChatMessage> rows=messages.latestMessages(id,PageRequest.of(0,Math.max(1,Math.min(50,limit)))); Collections.reverse(rows); return rows; }

    @Transactional(readOnly = true)
    public boolean aiDailyLimitReached(Integer memberUserId) {
        java.time.LocalDate today = clock.instant().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        LocalDateTime start = today.atStartOfDay();
        return messages.countAiRepliesForMemberBetween(memberUserId, start, start.plusDays(1)) >= Math.max(0, aiDailyLimitPerMember);
    }

    private ChatConversationResponse response(ChatConversation c,Integer viewerId) {
        List<ChatMessage> last=messages.findByConversation_IdOrderByIdDesc(c.getId(),PageRequest.of(0,1));
        boolean staff=viewerId!=null && !c.getMember().getId().equals(viewerId);
        return ChatConversationResponse.from(c,last.isEmpty()?null:last.get(0),viewerId,unread(c,viewerId,staff));
    }
    private void closeConversation(ChatConversation conversation, LocalDateTime closedAt) {
        conversation.setStatus(ChatConversationStatus.CLOSED);
        conversation.setClosedAt(closedAt);
        conversation.setUpdatedAt(closedAt);
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
        if(isMember(user) && !"Active".equalsIgnoreCase(user.getStatus())) throw new org.springframework.security.access.AccessDeniedException("T\u00e0i kho\u1ea3n th\u00e0nh vi\u00ean ch\u01b0a ho\u1ea1t \u0111\u1ed9ng");
        if(!isMember(user) && (!isStaff(user) || !hasPermission(auth,"HANDLE_CHAT"))) throw new org.springframework.security.access.AccessDeniedException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp cu\u1ed9c tr\u00f2 chuy\u1ec7n");
        return c;
    }
    private User currentUser(Authentication auth) {
        if(auth==null || !auth.isAuthenticated()) throw new org.springframework.security.access.AccessDeniedException("Vui l\u00f2ng \u0111\u0103ng nh\u1eadp \u0111\u1ec3 ti\u1ebfp t\u1ee5c");
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new ResourceNotFoundException("Authenticated user not found"));
    }
    private User requireMember(Authentication auth) { User u=currentUser(auth); if(!isMember(u)||!"Active".equalsIgnoreCase(u.getStatus())) throw new org.springframework.security.access.AccessDeniedException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n s\u1eed d\u1ee5ng tr\u00f2 chuy\u1ec7n th\u00e0nh vi\u00ean"); return u; }
    private User requireStaff(Authentication auth) { User u=currentUser(auth); if(!isStaff(u)||!hasPermission(auth,"HANDLE_CHAT")) throw new org.springframework.security.access.AccessDeniedException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n x\u1eed l\u00fd cu\u1ed9c tr\u00f2 chuy\u1ec7n"); return u; }
    private boolean isMember(User u) { return u.getRole()!=null && "MEMBER".equalsIgnoreCase(u.getRole().getName()); }
    private boolean isStaff(User u) { return "Active".equalsIgnoreCase(u.getStatus()) && u.getRole()!=null && ("RECEPTIONIST".equalsIgnoreCase(u.getRole().getName())||"CENTERMANAGER".equalsIgnoreCase(u.getRole().getName())); }
    private boolean hasPermission(Authentication a,String permission) { return a!=null&&a.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(permission::equals); }
    private String cleanSubject(String s) { if(s==null||s.isBlank())return null; String v=s.trim(); return v.length()>200?v.substring(0,200):v; }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private static ChatException notFound() { return new ChatException(404,"CHAT_CONVERSATION_NOT_FOUND","Kh\u00f4ng t\u00ecm th\u1ea5y cu\u1ed9c tr\u00f2 chuy\u1ec7n"); }
}
