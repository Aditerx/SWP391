package com.sportscenter.session;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.membership.*;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.audit.AuditService;
import com.sportscenter.notification.*;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CenterIncidentService {
    private final CenterIncidentRepository incidents;
    private final SessionRepository sessions;
    private final MemberPackageRepository subscriptions;
    private final SubscriptionPauseRepository pauses;
    private final EntitlementTimePolicy timePolicy;
    private final ClassEnrollmentRepository enrollments;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final com.sportscenter.invoice.InvoiceRepository invoices;

    public record Response(Integer id,Integer sessionId,String status,LocalDateTime startedAt,LocalDateTime endedAt,
            LocalDateTime originalDeadline,LocalDateTime deadlineBefore,LocalDateTime deadlineAfter,int recoveredCredits,
            boolean sessionCompensated,Set<Integer> affectedMemberIds,String reason,String evidence) {
        static Response from(CenterIncident i){return new Response(i.getId(),i.getSession().getId(),i.getStatus(),i.getStartedAt(),i.getEndedAt(),
                i.getOriginalDeadline(),i.getDeadlineBefore(),i.getDeadlineAfter(),i.getRecoveredCredits(),i.isSessionCompensated(),Set.copyOf(i.getAffectedMembers()),i.getReason(),i.getEvidence());}
    }

    @Transactional
    public Response open(Integer sessionId,LocalDateTime startedAt,String reason,String evidence,String key,Authentication auth) {
        User actor=actors.actor(auth);ActorPolicy.manager(actor);ActorPolicy.reason(reason);ActorPolicy.reason(evidence);AdvancedRegistrationService.requireKey(key);
        mutex.acquire(actor.getCenterId());Session s=locked(sessionId);ActorPolicy.center(actor,s.getSportsClass().getCenterId());
        if("Cancelled".equals(s.getStatus())&&!s.isCenterInvalidated())throw new BusinessException("SESSION_ALREADY_CANCELLED: A Member-cancelled session has no center compensation entitlement");
        var previous=incidents.findBySessionIdAndRequestKey(sessionId,key);
        if(previous.isPresent()) {
            var p=previous.get();
            if(!Objects.equals(p.getStartedAt(),startedAt)||!p.getReason().equals(reason)||!p.getEvidence().equals(evidence))throw new BusinessConflictException("IDEMPOTENCY_CONFLICT: Different incident content");
            return Response.from(p);
        }
        LocalDateTime now=LocalDateTime.now(clock);
        if(startedAt==null||startedAt.isAfter(now))throw new BusinessException("INVALID_INCIDENT_TIME: Actual incident start must not be in the future");
        CenterIncident i=new CenterIncident();i.setSession(s);i.setStartedAt(startedAt);i.setReason(reason);i.setEvidence(evidence);i.setRequestKey(key);i.setCreatedBy(actor.getId());i.setCreatedAt(now);
        if(s.getBookedMember()!=null){
            if(s.getMemberPackage()==null)throw new BusinessException("SUBSCRIPTION_LINK_REQUIRED: Reconcile original subscription");
            MemberPackage sub=subscriptions.findByIdForUpdate(s.getMemberPackage().getSubscriptionId()).orElseThrow();
            requirePaid(sub);
            if(sub.getActivatedAt()==null||sub.getExpiresAt()==null||startedAt.isBefore(sub.getActivatedAt())||!startedAt.isBefore(sub.getExpiresAt())||"Cancelled".equals(sub.getStatus()))
                throw new BusinessException("INCIDENT_OUTSIDE_ENTITLEMENT: Incident must affect an existing valid entitlement");
            i.setOriginalDeadline(sub.getExpiresAt());i.getAffectedMembers().add(s.getBookedMember().getId());
        }else{
            enrollments.findByClassIdWithDetails(s.getSportsClass().getId()).stream().filter(e->List.of("Registered","Completed").contains(e.getStatus()))
                    .filter(e->invoices.existsByMemberIdAndSportsClassIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(e.getMember().getId(),s.getSportsClass().getId(),"Paid"))
                    .forEach(e->i.getAffectedMembers().add(e.getMember().getId()));
            if(i.getAffectedMembers().isEmpty())throw new BusinessException("NO_AFFECTED_ENTITLEMENT: No paid enrollment is affected");
        }
        incidents.save(i);audit.log(actor.getId(),"OPEN_CENTER_INCIDENT","SESSION",sessionId,"incident="+i.getId()+"; start="+startedAt+"; reason="+reason+"; evidence="+evidence);
        return Response.from(i);
    }

    @Transactional
    public Response resolve(Integer id,LocalDateTime endedAt,Authentication auth) {
        User actor=actors.actor(auth);ActorPolicy.manager(actor);mutex.acquire(actor.getCenterId());
        CenterIncident i=entity(id);ActorPolicy.center(actor,i.getSession().getSportsClass().getCenterId());locked(i.getSession().getId());
        if("Resolved".equals(i.getStatus())){
            if(!Objects.equals(endedAt,i.getEndedAt()))throw new BusinessConflictException("INCIDENT_ALREADY_RESOLVED: Approved history is immutable");
            return Response.from(i);
        }
        if(endedAt==null||endedAt.isBefore(i.getStartedAt())||endedAt.isAfter(LocalDateTime.now(clock)))throw new BusinessException("INVALID_INCIDENT_TIME: Supply the actual recovery time between start and now");
        i.setEndedAt(endedAt);i.setStatus("Resolved");i.setResolvedBy(actor.getId());i.setResolvedAt(LocalDateTime.now(clock));
        MemberPackage original=i.getSession().getMemberPackage();
        if(original!=null){
            MemberPackage sub=subscriptions.findByIdForUpdate(original.getSubscriptionId()).orElseThrow();
            requirePaid(sub);
            if("Cancelled".equals(sub.getStatus()))throw new BusinessException("ENTITLEMENT_RECONCILIATION_REQUIRED: Cancelled entitlement cannot be restored");
            snapshotExpiryIfDue(sub);
            i.setDeadlineBefore(sub.getExpiresAt());
            if(endedAt.isAfter(i.getStartedAt())){
                SubscriptionPause pause=new SubscriptionPause();pause.setSubscription(sub);pause.setSourceKey("INCIDENT:"+id);pause.setStartsAt(i.getStartedAt());pause.setEndsAt(endedAt);pauses.saveAndFlush(pause);
            }
            timePolicy.recalculate(sub);i.setRecoveredCredits(restoreIfValid(sub));i.setDeadlineAfter(sub.getExpiresAt());
            audit.log(actor.getId(),"RESOLVE_CENTER_INCIDENT","MEMBER_PACKAGE",sub.getSubscriptionId(),"incident="+id+"; deadline="+i.getDeadlineBefore()+" -> "+i.getDeadlineAfter()+"; restored="+i.getRecoveredCredits());
        }
        notify(i,"Sự cố đã được xác minh và chốt thời gian khắc phục.","RESOLVED");
        return Response.from(i);
    }

    /** Called only within a validated booking/reschedule transaction. Extends exactly to the agreed session end. */
    public void approveMakeupEnd(Integer incidentId,MemberPackage sub,LocalDateTime targetEnd,String reason,User actor) {
        ActorPolicy.manager(actor);ActorPolicy.reason(reason);
        requirePaid(sub);
        CenterIncident i=entity(incidentId);
        if(i.getSession().getMemberPackage()==null||!Objects.equals(i.getSession().getMemberPackage().getSubscriptionId(),sub.getSubscriptionId())||!"Resolved".equals(i.getStatus()))
            throw new BusinessException("INCIDENT_RECOVERY_REQUIRED: Resolved incident for this subscription required");
        ActorPolicy.center(actor,sub.getMember().getCenterId());
        if("Cancelled".equals(sub.getStatus())||i.getOriginalDeadline()==null||!i.getStartedAt().isBefore(i.getOriginalDeadline()))throw new BusinessException("INCIDENT_OUTSIDE_ENTITLEMENT: No valid recovery evidence");
        snapshotExpiryIfDue(sub);
        if(targetEnd.isAfter(sub.getExpiresAt())){
            Duration additional=EntitlementTimePolicy.openTimeBetween(sub.getExpiresAt(),targetEnd,timePolicy.intervals(sub));
            Duration total=Duration.ofSeconds(sub.getApprovedExtraSeconds()).plusNanos(sub.getApprovedExtraNanos()).plus(additional);
            sub.setApprovedExtraSeconds(total.getSeconds());sub.setApprovedExtraNanos(total.getNano());
            timePolicy.recalculate(sub);
            if(!targetEnd.equals(sub.getExpiresAt()))throw new BusinessException("INVALID_MAKEUP_DEADLINE: Target end overlaps a closure");
            i.setDeadlineAfter(targetEnd);
            audit.log(actor.getId(),"APPROVE_MINIMUM_MAKEUP_EXTENSION","MEMBER_PACKAGE",sub.getSubscriptionId(),"incident="+incidentId+"; targetEnd="+targetEnd+"; openDuration="+additional+"; reason="+reason);
        }
        int restored=restoreIfValid(sub);i.setRecoveredCredits(i.getRecoveredCredits()+restored);
    }

    @Transactional
    public Response compensateUnheldSession(Integer id,String reason,boolean verifiedNotHeld,Authentication auth){
        User actor=actors.actor(auth);ActorPolicy.manager(actor);ActorPolicy.reason(reason);mutex.acquire(actor.getCenterId());
        CenterIncident i=entity(id);Session s=locked(i.getSession().getId());ActorPolicy.center(actor,s.getSportsClass().getCenterId());
        if(!verifiedNotHeld)throw new BusinessException("NOT_HELD_CONFIRMATION_REQUIRED: Verify that the session was not held at all");
        if(i.isSessionCompensated()||s.isCenterInvalidated())return Response.from(i);
        if(s.getSessionDate().atTime(s.getStartTime()).isAfter(LocalDateTime.now(clock))){s.setStatus("Cancelled");}
        // Preserve historical attendance/status; this flag excludes the session from entitlement/no-show counts.
        s.setCenterInvalidated(true);
        if(s.getBookedMember()!=null){
            MemberPackage sub=subscriptions.findByIdForUpdate(s.getMemberPackage().getSubscriptionId()).orElseThrow();
            if("Cancelled".equals(sub.getStatus()))throw new BusinessException("ENTITLEMENT_RECONCILIATION_REQUIRED: Subscription cancelled");
            snapshotExpiryIfDue(sub);
            if(!s.isCreditReturned()){
                if(sub.getExpiresAt()!=null&&sub.getExpiresAt().isAfter(LocalDateTime.now(clock))){
                    int credit=sub.getRemainingCredit()==null?0:sub.getRemainingCredit();
                    if(credit>=12)throw new BusinessConflictException("CREDIT_LIMIT_EXCEEDED: Reconcile original credit");
                    sub.setRemainingCredit(credit+1);sub.setStatus("Active");
                }else{sub.setDeferredCreditReturn(sub.getDeferredCreditReturn()+1);}
                s.setCreditReturned(true);
            }
        }else{
            // Re-open the same obligation; the linked makeup later replaces this invalid session.
            enrollments.findByClassIdWithDetails(s.getSportsClass().getId()).stream().filter(e->i.getAffectedMembers().contains(e.getMember().getId())&&"Completed".equals(e.getStatus()))
                    .forEach(e->e.setStatus("Registered"));
        }
        i.setSessionCompensated(true);
        audit.log(actor.getId(),"COMPENSATE_UNHELD_SESSION","SESSION",s.getId(),"incident="+id+"; reason="+reason+"; preserved history; credit returned="+s.isCreditReturned());
        notify(i,"Buổi học không tổ chức do trung tâm đã được ghi nhận để bảo toàn quyền học.","COMPENSATED");
        return Response.from(i);
    }
    @Transactional(readOnly=true) public List<Response> list(Authentication auth){User actor=actors.actor(auth);ActorPolicy.manager(actor);return incidents.findBySessionSportsClassCenterId(actor.getCenterId()).stream().map(Response::from).toList();}

    public CenterIncident requireForSession(Integer id,Session session){
        if(id==null)throw new BusinessException("INCIDENT_REQUIRED: Center incident evidence is required");
        CenterIncident i=entity(id);if(!i.getSession().getId().equals(session.getId()))throw new BusinessException("INCIDENT_SESSION_MISMATCH: Incident belongs to another session");return i;
    }
    private int restoreIfValid(MemberPackage sub){
        if(sub.getExpiresAt()==null||!sub.getExpiresAt().isAfter(LocalDateTime.now(clock)))return 0;
        int revoked=sub.getExpiryRevokedCredit()==null?0:sub.getExpiryRevokedCredit();
        if(revoked>0&&sub.getExpiryProcessedAt()==null)throw new BusinessException("EXPIRY_EVIDENCE_REQUIRED: Missing revoked credit snapshot timestamp");
        int recovered=revoked+sub.getDeferredCreditReturn();
        int remaining=sub.getRemainingCredit()==null?0:sub.getRemainingCredit();
        if(recovered+remaining>12)throw new BusinessConflictException("CREDIT_LIMIT_EXCEEDED: Reconcile credit evidence");
        if(recovered>0){sub.setRemainingCredit(remaining+recovered);sub.setExpiryRevokedCredit(0);sub.setDeferredCreditReturn(0);}
        if(("Expired".equals(sub.getStatus())||"Completed".equals(sub.getStatus()))&&(sub.getRemainingCredit()>0||sessions.countByMemberPackageSubscriptionIdAndStatusAndAttendanceFinalizedFalse(sub.getSubscriptionId(),"Scheduled")>0))sub.setStatus("Active");
        return recovered;
    }
    public void snapshotExpiryIfDue(MemberPackage sub){
        if("Active".equals(sub.getStatus())&&sub.getExpiresAt()!=null&&!sub.getExpiresAt().isAfter(LocalDateTime.now(clock))){
            sub.setExpiryRevokedCredit(sub.getRemainingCredit()==null?0:sub.getRemainingCredit());sub.setExpiryProcessedAt(LocalDateTime.now(clock));sub.setRemainingCredit(0);sub.setStatus("Expired");
        }
    }
    private CenterIncident entity(Integer id){return incidents.findById(id).orElseThrow(()->new ResourceNotFoundException("Incident not found"));}
    private void requirePaid(MemberPackage sub){if(!invoices.existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(sub.getSubscriptionId(),"Paid"))throw new BusinessException("PAID_ENTITLEMENT_REQUIRED: Original non-reconciled, non-reversed payment evidence is required");}
    private Session locked(Integer id){return sessions.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Session not found"));}
    private void notify(CenterIncident i,String text,String outcome){events.publishEvent(NotificationTemplates.event(NotificationType.CENTER_INCIDENT_UPDATED,List.copyOf(i.getAffectedMembers()),text,"SESSION",i.getSession().getId(),"CENTER_INCIDENT:"+i.getId()+":"+outcome));}
}
