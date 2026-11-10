package com.sportscenter.normal;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.sportclass.*;
import com.sportscenter.session.*;
import com.sportscenter.enrollment.*;
import com.sportscenter.invoice.*;
import com.sportscenter.user.*;
import com.sportscenter.membership.CoachCapacityService;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.audit.AuditService;
import com.sportscenter.notification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class NormalChangeService {
    private final SportsClassRepository classes;
    private final SessionRepository sessions;
    private final ClassEnrollmentRepository enrollments;
    private final InvoiceRepository invoices;
    private final NormalProposalRepository proposals;
    private final NormalResponseRepository responses;
    private final NormalTransferRepository transfers;
    private final NormalRefundService refunds;
    private final PaymentResolutionRepository resolutions;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final CoachCapacityService coaches;
    private final MemberRepository members;
    private final SessionSlotValidator slots;
    private final HolidayRepository holidays;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeProposalInput")
    public record ProposalInput(String kind,Integer targetClassId,Integer shiftDays,long version,String reason) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeChoice")
    public record Choice(long version,String fingerprint,String choice) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeDecision")
    public record Decision(long version,String reason) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeScheduleItem")
    public record ScheduleItem(Integer sessionId,LocalDate date,LocalTime start,LocalTime end,Integer coachId,Integer roomId) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeProposalView")
    public record ProposalView(Integer id,Integer classId,Integer targetClassId,String kind,String status,Integer shiftDays,
            long version,String fingerprint,String reason,List<ScheduleItem> schedule,List<MemberChoice> choices) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalChangeMemberChoice")
    public record MemberChoice(Integer memberId,String choice,LocalDateTime respondedAt) {}

    @Transactional public ProposalView propose(Integer classId,ProposalInput input,Authentication auth) {
        User actor=manager(auth);ActorPolicy.reason(input.reason());mutex.acquire(actor.getCenterId());
        lockClasses(classId,input.targetClassId());SportsClass c=classes.findById(classId).orElseThrow();ActorPolicy.center(actor,c.getCenterId());prestart(c);version(c,input.version());
        if(c.isChangePending())throw new BusinessConflictException("NORMAL_CHANGE_PENDING: Resolve or cancel the existing proposal");
        if(input.kind()==null||!Set.of("Postpone","Transfer").contains(input.kind()))throw new BusinessException("PROPOSAL_KIND_INVALID");
        if("Postpone".equals(input.kind())) {
            if(input.targetClassId()!=null||input.shiftDays()==null||input.shiftDays()<1||input.shiftDays()>366||c.getPostponementCount()!=0)throw new BusinessException("NORMAL_POSTPONEMENT_LIMIT: Only one postponement with a future schedule is allowed");
            if(validPaid(c).size()>=c.getMinCapacity())throw new BusinessException("NORMAL_POSTPONEMENT_REASON: This postponement is for insufficient paid enrollment");
            validateSchedule(c,input.shiftDays(),null);
        } else {
            if(input.targetClassId()==null||input.targetClassId().equals(classId))throw new BusinessException("TRANSFER_TARGET_REQUIRED");
            SportsClass target=classes.findById(input.targetClassId()).orElseThrow();compatible(c,target,null);validateSchedule(target,0,null);
        }
        c.setChangePending(true);c.setOpeningConfirmedAt(null);c.setOpeningConfirmedBy(null);c.setOpeningReason(null);c.setLifecycleVersion(c.getLifecycleVersion()+1);
        // Stop existing unpaid holds; a late real receipt is reconciled without being treated as consent.
        cancelUnpaid(c);
        NormalProposal p=new NormalProposal();p.setClassId(classId);p.setTargetClassId(input.targetClassId());p.setKind(input.kind());p.setShiftDays("Postpone".equals(input.kind())?input.shiftDays():null);p.setVersion(c.getLifecycleVersion());p.setReason(input.reason());p.setCreatedBy(actor.getId());p.setCreatedAt(now());p.setFingerprint(fingerprint(c,p));p.setScheduleJson(snapshot(p));proposals.saveAndFlush(p);
        for(Invoice i:validPaid(c)) {NormalResponse r=new NormalResponse();r.setProposalId(p.getId());r.setInvoiceId(i.getInvoiceId());r.setMemberId(i.getMember().getId());r.setChoice("Waiting");r.setVersion(p.getVersion());r.setRespondedAt(now());responses.save(r);notify(i.getMember().getId(),"Center proposes "+p.getKind()+"; inspect the concrete schedule and choose Accept or Refund",classId,"PROPOSAL:"+p.getId());}
        audit.log(actor.getId(),"PROPOSE_NORMAL_CHANGE","CLASS",classId,"proposal="+p.getId()+"; kind="+p.getKind()+"; reason="+input.reason());return view(p,actor);
    }
    @Transactional(readOnly=true) public List<ProposalView> list(Integer classId,Authentication auth) {
        User actor=actors.actor(auth);SportsClass c=classes.findById(classId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));ActorPolicy.center(actor,c.getCenterId());
        if(!ActorPolicy.role(actor,"Member"))manager(auth);
        return proposals.findByClassIdOrderByIdDesc(classId).stream().filter(p -> !ActorPolicy.role(actor,"Member") || responses.findByProposalId(p.getId()).stream().anyMatch(r -> actor.getId().equals(r.getMemberId()))).map(p -> view(p,actor)).toList();
    }
    @Transactional public ProposalView respond(Integer id,Choice input,Authentication auth) {
        User actor=actors.actor(auth);if(!ActorPolicy.role(actor,"Member"))throw new AccessDeniedException("Member consent required");mutex.acquire(actor.getCenterId());
        NormalProposal p=proposal(id);lockClasses(p.getClassId(),p.getTargetClassId());SportsClass c=classes.findById(p.getClassId()).orElseThrow();ActorPolicy.center(actor,c.getCenterId());
        NormalResponse r=responses.findByProposalId(id).stream().filter(x -> actor.getId().equals(x.getMemberId())).findFirst().orElseThrow(() -> new AccessDeniedException("Own paid proposal required"));
        if(!"Waiting".equals(r.getChoice())) {if(r.getChoice().equals(input.choice()))return view(p,actor);throw new BusinessConflictException("NORMAL_CHOICE_ALREADY_RECORDED");}
        prestart(c);checkProposal(c,p,input.version(),input.fingerprint(),!"Refund".equals(input.choice()));
        Invoice invoice=invoices.findByIdForUpdate(r.getInvoiceId()).orElseThrow();
        if(input.choice()==null||!Set.of("Accept","Refund").contains(input.choice()))throw new BusinessException("NORMAL_CHOICE_INVALID: Use Accept or Refund");
        if("Refund".equals(input.choice()))refunds.obligation(invoice,c,"MemberChoice","Member declined center proposal "+id,actor.getId());
        else if("Transfer".equals(p.getKind()))transfer(p,c,classes.findById(p.getTargetClassId()).orElseThrow(),invoice,actor);
        r.setChoice(input.choice());r.setRespondedAt(now());audit.log(actor.getId(),"RESPOND_NORMAL_CHANGE","CLASS",c.getId(),"proposal="+id+"; choice="+input.choice());
        return view(p,actor);
    }
    @Transactional public ProposalView apply(Integer id,Decision input,Authentication auth) {
        User actor=manager(auth);mutex.acquire(actor.getCenterId());NormalProposal p=proposal(id);lockClasses(p.getClassId(),p.getTargetClassId());SportsClass c=classes.findById(p.getClassId()).orElseThrow();ActorPolicy.center(actor,c.getCenterId());
        if("Applied".equals(p.getStatus()))return view(p,actor);
        prestart(c);checkProposal(c,p,input.version(),p.getFingerprint());
        if(responses.findByProposalId(id).stream().anyMatch(r -> "Waiting".equals(r.getChoice())))throw new BusinessException("MEMBER_RESPONSES_PENDING: Silence is not consent");
        if("Postpone".equals(p.getKind())) {
            validateSchedule(c,p.getShiftDays(),null);
            for(Session s:NormalClassPolicy.valid(c,sessions))s.setSessionDate(s.getSessionDate().plusDays(p.getShiftDays()));
            var schedule=NormalClassPolicy.valid(c,sessions);c.setStartDate(schedule.get(0).getSessionDate());c.setEndDate(schedule.get(11).getSessionDate());c.setPostponementCount(c.getPostponementCount()+1);c.setChangePending(false);
        }
        p.setStatus("Applied");c.setLifecycleVersion(c.getLifecycleVersion()+1);
        audit.log(actor.getId(),"APPLY_NORMAL_CHANGE","CLASS",c.getId(),"proposal="+id+"; no opening confirmation implied");return view(p,actor);
    }
    @Transactional public SportsClassResponse cancel(Integer classId,Decision input,Authentication auth) {
        User actor=manager(auth);ActorPolicy.reason(input.reason());mutex.acquire(actor.getCenterId());SportsClass c=classes.findByIdForUpdate(classId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));ActorPolicy.center(actor,c.getCenterId());
        if("Cancelled".equals(c.getStatus()))return SportsClassResponse.from(c);
        prestart(c);version(c,input.version());
        c.setStatus("Cancelled");c.setChangePending(false);c.setLifecycleVersion(c.getLifecycleVersion()+1);cancelUnpaid(c);
        // Include reconciled late receipts: money received is never silently discarded.
        for(Invoice i:invoices.findEffectiveClassInvoices(classId))if("Paid".equals(i.getPaymentStatus())&&!i.isCashConfirmationReversed())refunds.obligation(invoices.findByIdForUpdate(i.getInvoiceId()).orElseThrow(),c,"CenterCancellation",input.reason(),actor.getId());
        for(ClassEnrollment e:enrollments.findByClassIdWithDetails(classId)){e.setStatus("Cancelled");e.setCancelledAt(now());e.setCancelReason(input.reason());notify(e.getMember().getId(),"Center cancelled the class; genuine receipts have refund obligations",classId,"CANCEL:"+classId);}
        for(Session s:sessions.findBySportsClassId(classId))if("Scheduled".equals(s.getStatus()))s.setStatus("Cancelled");
        for(NormalProposal p:proposals.findByClassIdOrderByIdDesc(classId))if("Pending".equals(p.getStatus()))p.setStatus("Cancelled");
        audit.log(actor.getId(),"CANCEL_NORMAL_PRESTART","CLASS",classId,"reason="+input.reason());return SportsClassResponse.from(c);
    }
    private void transfer(NormalProposal p,SportsClass source,SportsClass target,Invoice invoice,User member) {
        if(!"Paid".equals(invoice.getPaymentStatus()) || invoice.isReconciliationRequired() || invoice.isCashConfirmationReversed()
                || invoice.effectiveClass()==null || !source.getId().equals(invoice.effectiveClass().getId()))
            throw new BusinessConflictException("NORMAL_RECEIPT_NOT_ELIGIBLE: Recheck the original paid entitlement before transfer");
        compatible(source,target,invoice);validateSchedule(target,0,member.getId(),source.getId());
        Member profile=members.findById(member.getId()).orElseThrow(() -> new BusinessException("MEMBER_PROFILE_REQUIRED"));
        if(profile.getBookingBlockedUntil()!=null&&profile.getBookingBlockedUntil().isAfter(now()))throw new BusinessException("BOOKING_BLOCKED");
        if(resolutions.findByInvoiceInvoiceId(invoice.getInvoiceId()).stream().anyMatch(r -> "Refund".equals(r.getType())))throw new BusinessConflictException("REFUND_ALREADY_EXISTS");
        var old=enrollments.findByMemberIdAndSportsClassId(member.getId(),source.getId()).orElseThrow();
        if(!"Registered".equals(old.getStatus()))throw new BusinessConflictException("NORMAL_RIGHT_NOT_ACTIVE");
        // Expire target holds under the same center mutex without involving EnrollmentService (no dependency cycle).
        for(Invoice i:invoices.findEffectiveClassInvoices(target.getId()))if("Pending".equals(i.getPaymentStatus())&&i.getExpiresAt()!=null&&!now().isBefore(i.getExpiresAt())) {i.setPaymentStatus("Expired");enrollments.findByMemberIdAndSportsClassId(i.getMember().getId(),target.getId()).filter(e -> "Pending".equals(e.getStatus())).ifPresent(e -> e.setStatus("Cancelled"));}
        if(enrollments.countBySportsClassIdAndStatusIn(target.getId(),List.of("Registered","Pending"))>=target.getMaxCapacity())throw new BusinessConflictException("CLASS_FULL: Preview does not reserve capacity");
        ClassEnrollment dest=enrollments.findByMemberIdAndSportsClassId(member.getId(),target.getId()).orElse(null);
        if(dest!=null&&List.of("Registered","Pending","Completed").contains(dest.getStatus()))throw new BusinessConflictException("ALREADY_ENROLLED");
        if(dest==null){dest=new ClassEnrollment();dest.setSportsClass(target);dest.setMember(member);}dest.setStatus("Registered");dest.setEnrolledAt(now());dest.setCancelledAt(null);dest.setCancelReason(null);dest.setCancelledBy(null);enrollments.saveAndFlush(dest);
        old.setStatus("Cancelled");old.setCancelledAt(now());old.setCancelReason("Transferred by Member consent; proposal="+p.getId());invoice.setEntitlementClass(target);
        NormalTransfer t=new NormalTransfer();t.setProposalId(p.getId());t.setInvoiceId(invoice.getInvoiceId());t.setSourceEnrollmentId(old.getId());t.setTargetEnrollmentId(dest.getId());t.setConsentedBy(member.getId());t.setTransferredAt(now());transfers.save(t);
        notify(member.getId(),"Transfer committed; original invoice retained",target.getId(),"TRANSFER:"+p.getId()+":"+invoice.getInvoiceId());
    }
    private void compatible(SportsClass source,SportsClass target,Invoice invoice) {
        if(!target.isNormal() || !Objects.equals(source.getCenterId(),target.getCenterId()) || !Objects.equals(source.getSubject().getId(),target.getSubject().getId()) || target.getTuitionFee().compareTo(invoice==null?source.getTuitionFee():invoice.getAmount())!=0)throw new BusinessException("NORMAL_TRANSFER_INCOMPATIBLE: Same center, subject and paid price required");
        NormalClassPolicy.requireAdmission(target,sessions,clock);
    }
    private void validateSchedule(SportsClass c,int shift,Integer member) { validateSchedule(c,shift,member,null); }
    private void validateSchedule(SportsClass c,int shift,Integer member,Integer relinquishedClass) {
        var schedule=NormalClassPolicy.valid(c,sessions);
        if(schedule.size()!=12 || schedule.stream().anyMatch(s -> !"Scheduled".equals(s.getStatus())))throw new BusinessException("CLASS_NOT_READY");
        if(!schedule.get(0).getSessionDate().plusDays(shift).atTime(schedule.get(0).getStartTime()).minusHours(48).isAfter(now()))throw new BusinessException("NORMAL_REGISTRATION_CLOSED: Proposed schedule must leave a future registration cutoff");
        for(Session s:schedule){LocalDate day=s.getSessionDate().plusDays(shift);User coach=s.getCoach()==null?c.getCoach():s.getCoach();var room=s.getRoom();
            if(c.getSubject()==null||!"Active".equals(c.getSubject().getStatus())||coach==null||room==null||!Objects.equals(room.getCenterId(),c.getCenterId())||!"Available".equals(room.getStatus())||room.getCapacity()<c.getMaxCapacity())throw new BusinessException("NORMAL_RESOURCE_UNAVAILABLE");
            coaches.requireCoach(coach,c.getSubject(),c.getCenterId());coaches.requireWorking(coach.getId(),day,s.getStartTime());com.sportscenter.room.RoomActivity.require(room,c.getSubject());slots.validateSession(c,room,s.getStartTime(),s.getEndTime());
            if(holidays.existsByHolidayDateAndIsActiveTrue(day))throw new BusinessException("HOLIDAY_BLOCKED");
            if(sessions.findCoachConflicts(coach.getId(),day,s.getStartTime(),s.getEndTime(),s.getId()).stream().anyMatch(x -> !c.getId().equals(x.getSportsClass().getId()))||sessions.findRoomConflicts(room.getId(),day,s.getStartTime(),s.getEndTime(),s.getId()).stream().anyMatch(x -> !c.getId().equals(x.getSportsClass().getId())))throw new BusinessConflictException("NORMAL_RESOURCE_CONFLICT");
            List<Integer> people=member==null?enrollments.findByClassIdWithDetails(c.getId()).stream().filter(e -> "Registered".equals(e.getStatus())).map(e -> e.getMember().getId()).toList():List.of(member);
            for(Integer person:people)if(sessions.findMemberConflicts(person,day,s.getStartTime(),s.getEndTime(),s.getId()).stream().anyMatch(x -> !c.getId().equals(x.getSportsClass().getId())&&!Objects.equals(relinquishedClass,x.getSportsClass().getId())))throw new BusinessConflictException("SCHEDULE_CONFLICT");
        }
    }
    private void prestart(SportsClass c){if(!c.isNormal()||!"Open".equals(c.getStatus())||c.isPricingDraft()||c.getMinCapacity()==null||!NormalClassPolicy.first(c,sessions).isAfter(now()))throw new BusinessException("NORMAL_PRESTART_REQUIRED: Published Normal before its first session required");}
    private void cancelUnpaid(SportsClass c){for(Invoice i:invoices.findEffectiveClassInvoices(c.getId()))if("Pending".equals(i.getPaymentStatus()))i.setPaymentStatus("Cancelled");for(var e:enrollments.findByClassIdWithDetails(c.getId()))if("Pending".equals(e.getStatus())){e.setStatus("Cancelled");e.setCancelledAt(now());e.setCancelReason("Center change invalidated unpaid reservation");}}
    private List<Invoice> validPaid(SportsClass c){return invoices.findEffectiveClassInvoices(c.getId()).stream().filter(i -> "Paid".equals(i.getPaymentStatus())&&!i.isReconciliationRequired()&&!i.isCashConfirmationReversed()).filter(i -> enrollments.existsByMemberIdAndSportsClassIdAndStatus(i.getMember().getId(),c.getId(),"Registered")).toList();}
    private void checkProposal(SportsClass c,NormalProposal p,long version,String fingerprint){checkProposal(c,p,version,fingerprint,true);}
    private void checkProposal(SportsClass c,NormalProposal p,long version,String fingerprint,boolean recheckSchedule){if(!"Pending".equals(p.getStatus())||p.getVersion()!=version||c.getLifecycleVersion()!=version||!p.getFingerprint().equals(fingerprint)||recheckSchedule&&!p.getFingerprint().equals(fingerprint(c,p)))throw new BusinessConflictException("NORMAL_PROPOSAL_STALE: Reload the current proposal");}
    private void version(SportsClass c,long v){if(c.getLifecycleVersion()!=v)throw new BusinessConflictException("NORMAL_VERSION_STALE");}
    private void lockClasses(Integer a,Integer b){java.util.stream.Stream.of(a,b).filter(Objects::nonNull).distinct().sorted().forEach(id -> classes.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Class not found")));}
    private String fingerprint(SportsClass c,NormalProposal p){StringBuilder text=new StringBuilder();for(Integer id:java.util.stream.Stream.of(c.getId(),p.getTargetClassId()).filter(Objects::nonNull).distinct().sorted().toList()){SportsClass x=classes.findById(id).orElseThrow();text.append(id).append(':').append(x.getStatus()).append(':').append(x.getTuitionFee()).append(':').append(x.getMaxCapacity()).append(':').append(x.getMinCapacity()).append(':').append(x.getCoach()==null?null:x.getCoach().getId());for(Session s:NormalClassPolicy.valid(x,sessions))text.append('|').append(s.getId()).append(':').append(s.getSessionDate()).append(':').append(s.getStartTime()).append(':').append(s.getEndTime()).append(':').append(s.getRoom()==null?null:s.getRoom().getId()).append(':').append(s.getCoach()==null?null:s.getCoach().getId()).append(':').append(s.getStatus());}try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private NormalProposal proposal(Integer id){return proposals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));}
    private String snapshot(NormalProposal p) {
        SportsClass c=classes.findById(p.getTargetClassId()==null?p.getClassId():p.getTargetClassId()).orElseThrow();
        int shift=p.getShiftDays()==null?0:p.getShiftDays();
        var rows=NormalClassPolicy.valid(c,sessions).stream().map(s -> new ScheduleItem(s.getId(),s.getSessionDate().plusDays(shift),s.getStartTime(),s.getEndTime(),s.getCoach()==null?c.getCoach().getId():s.getCoach().getId(),s.getRoom().getId())).toList();
        try{return json.writeValueAsString(rows);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
    private ProposalView view(NormalProposal p,User actor) {
        List<ScheduleItem> rows;
        try{rows=json.readValue(p.getScheduleJson(),new com.fasterxml.jackson.core.type.TypeReference<List<ScheduleItem>>(){});}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Invalid proposal schedule snapshot",e);}
        return new ProposalView(p.getId(),p.getClassId(),p.getTargetClassId(),p.getKind(),p.getStatus(),p.getShiftDays(),p.getVersion(),p.getFingerprint(),p.getReason(),rows,responses.findByProposalId(p.getId()).stream().filter(r -> !ActorPolicy.role(actor,"Member")||actor.getId().equals(r.getMemberId())).map(r -> new MemberChoice(r.getMemberId(),r.getChoice(),r.getRespondedAt())).toList());
    }
    private User manager(Authentication auth){User u=actors.actor(auth);if(!ActorPolicy.role(u,"CenterManager")||auth.getAuthorities().stream().noneMatch(a -> "MANAGE_CLASSES".equals(a.getAuthority())))throw new AccessDeniedException("CenterManager with MANAGE_CLASSES required");return u;}
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    @Transactional public void remind(Integer classId) {
        Integer center=classes.findCenterIdById(classId).orElse(null);if(center==null)return;mutex.acquire(center);
        for(NormalProposal p:proposals.findByClassIdOrderByIdDesc(classId))if("Pending".equals(p.getStatus()))
            for(NormalResponse r:responses.findByProposalId(p.getId()))if("Waiting".equals(r.getChoice()))notify(r.getMemberId(),"A concrete center proposal still needs your choice; silence does not waive refund rights",classId,"REMINDER:"+p.getId()+":"+LocalDate.now(clock));
    }
    private void notify(Integer member,String message,Integer c,String key){events.publishEvent(NotificationTemplates.event(NotificationType.CENTER_INCIDENT_UPDATED,List.of(member),message,"CLASS",c,"NORMAL_CHANGE:"+key+":"+member));}
}
