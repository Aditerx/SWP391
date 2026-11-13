package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.membership.*;
import com.sportscenter.notification.*;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PrivateSessionProposalService {
    private final PrivateSessionProposalRepository proposals;
    private final MemberPackageRepository subscriptions;
    private final SessionRepository sessions;
    private final SessionService bookings;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public PrivateSessionProposalResponse propose(PrivateBookingRequest r,Authentication auth) {
        User actor=actors.actor(auth);
        AdvancedRegistrationService.requireKey(r.requestKey());
        Integer center=subscriptions.findCenterIdById(r.subscriptionId()).orElseThrow(()->new ResourceNotFoundException("Subscription not found"));
        ActorPolicy.center(actor,center);mutex.acquire(center);
        MemberPackage sub=subscriptions.findByIdForUpdate(r.subscriptionId()).orElseThrow();
        requireProposer(actor,sub,r.supportReason());
        String hash=hash(r.toString());
        var previous=proposals.findBySubscriptionSubscriptionIdAndRequestKey(r.subscriptionId(),r.requestKey());
        if(previous.isPresent()) {
            if(!previous.get().getRequestHash().equals(hash))throw new BusinessConflictException("IDEMPOTENCY_CONFLICT: Proposal key has a different schedule");
            return PrivateSessionProposalResponse.from(previous.get(),clock);
        }
        bookings.validatePrivateProposal(r,auth);
        PrivateSessionProposal p=new PrivateSessionProposal();p.setSubscription(sub);p.setProposedBy(actor);
        p.setCoachId(r.coachId());p.setRoomId(r.roomId());p.setClassId(r.classId());p.setSubjectId(r.subjectId());
        p.setSessionDate(r.sessionDate());p.setStartTime(r.startTime());p.setEndTime(r.endTime());
        p.setRequestKey(r.requestKey());p.setRequestHash(hash);p.setCreatedAt(LocalDateTime.now(clock));
        p.setRespondBy(r.sessionDate().atTime(r.startTime()).minusHours(12));p.setSupportReason(r.supportReason());
        p.setIncidentId(r.incidentId());p.setExtraValidityReason(r.extraValidityReason());proposals.saveAndFlush(p);
        audit.log(actor.getId(),"PROPOSE_PRIVATE_SESSION","PRIVATE_SESSION_PROPOSAL",p.getId(),"No resources or credit reserved");
        events.publishEvent(NotificationTemplates.event(NotificationType.PRIVATE_BOOKING_PROPOSED,List.of(sub.getMember().getId()),
                "Coach proposed "+p.getSessionDate()+" "+p.getStartTime()+"; confirm the exact schedule before "+p.getRespondBy(),
                "PRIVATE_SESSION_PROPOSAL",p.getId(),"PRIVATE_BOOKING_PROPOSED:"+p.getId()));
        return PrivateSessionProposalResponse.from(p,clock);
    }

    @Transactional(readOnly=true)
    public List<PrivateSessionProposalResponse> list(Authentication auth) {
        User actor=actors.actor(auth);requireReader(actor);
        return proposals.findBySubscriptionMemberCenterIdOrderByIdDesc(actor.getCenterId()).stream()
                .filter(p->visible(actor,p)).map(p->PrivateSessionProposalResponse.from(p,clock)).toList();
    }
    @Transactional(readOnly=true)
    public PrivateSessionProposalResponse get(Integer id,Authentication auth) {
        User actor=actors.actor(auth);requireReader(actor);PrivateSessionProposal p=entity(id);
        ActorPolicy.center(actor,p.getSubscription().getMember().getCenterId());
        if(!visible(actor,p))throw new AccessDeniedException("Proposal belongs to another account");
        return PrivateSessionProposalResponse.from(p,clock);
    }

    @Transactional
    public PrivateSessionProposalResponse respond(Integer id,boolean accepted,Authentication auth) {
        User member=actors.actor(auth);
        if(!ActorPolicy.role(member,"Member"))throw new AccessDeniedException("Member response required");
        Integer center=proposals.findCenterId(id).orElseThrow(()->new ResourceNotFoundException("Proposal not found"));
        ActorPolicy.center(member,center);mutex.acquire(center);
        PrivateSessionProposal initial=entity(id);
        MemberPackage sub=subscriptions.findByIdForUpdate(initial.getSubscription().getSubscriptionId()).orElseThrow();
        PrivateSessionProposal p=proposals.lockById(id).orElseThrow();
        if(!sub.getMember().getId().equals(member.getId()))throw new AccessDeniedException("Proposal owner required");
        String decision=accepted?"Accepted":"Rejected";
        if(!"Pending".equals(p.getStatus())) {
            if(decision.equals(p.getStatus()))return PrivateSessionProposalResponse.from(p,clock);
            throw new BusinessConflictException("PROPOSAL_STATE_CONFLICT: Proposal already processed");
        }
        if(LocalDateTime.now(clock).isAfter(p.getRespondBy()))throw new BusinessConflictException("PROPOSAL_EXPIRED: Schedule confirmation requires at least12hours notice");
        if(accepted) {
            // Re-evaluate the recorded proposer and all booking guards, never a client-supplied actor.
            Authentication proposer=new UsernamePasswordAuthenticationToken(p.getProposedBy().getEmail(),null,List.of());
            requireProposer(actors.actor(proposer),sub,p.getSupportReason());
            SessionResponse booked=bookings.bookPrivateSession(p.booking(),proposer);
            p.setSession(sessions.findById(booked.id()).orElseThrow());
        }
        p.setStatus(decision);p.setRespondedAt(LocalDateTime.now(clock));
        audit.log(member.getId(),accepted?"ACCEPT_PRIVATE_SESSION":"REJECT_PRIVATE_SESSION","PRIVATE_SESSION_PROPOSAL",id,decision);
        if(!accepted)events.publishEvent(NotificationTemplates.event(NotificationType.PRIVATE_BOOKING_DECLINED,List.of(p.getCoachId()),
                "Member declined the proposed schedule; no credit consumed","PRIVATE_SESSION_PROPOSAL",id,"PRIVATE_BOOKING_DECLINED:"+id));
        return PrivateSessionProposalResponse.from(p,clock);
    }
    private PrivateSessionProposal entity(Integer id){return proposals.findById(id).orElseThrow(()->new ResourceNotFoundException("Proposal not found"));}
    private void requireProposer(User actor,MemberPackage sub,String reason){
        if(ActorPolicy.role(actor,"Coach")&&sub.getPrimaryCoach()!=null&&actor.getId().equals(sub.getPrimaryCoach().getId()))return;
        ActorPolicy.manager(actor);ActorPolicy.reason(reason);
        if(actor.getRole().getPermissions().stream().noneMatch(p->"MANAGE_CLASSES".equals(p.getName())))throw new AccessDeniedException("MANAGE_CLASSES required");
    }
    private void requireReader(User actor){
        if(ActorPolicy.role(actor,"Member")||ActorPolicy.role(actor,"Coach"))return;
        ActorPolicy.manager(actor);
    }
    private boolean visible(User actor,PrivateSessionProposal p){
        if(ActorPolicy.role(actor,"Member"))return actor.getId().equals(p.getSubscription().getMember().getId());
        if(ActorPolicy.role(actor,"Coach"))return actor.getId().equals(p.getCoachId());
        return true;
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
