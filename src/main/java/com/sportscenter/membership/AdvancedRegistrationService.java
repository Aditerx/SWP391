package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.invoice.*;
import com.sportscenter.session.*;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdvancedRegistrationService {
    private final AdvancedRegistrationRepository registrations;
    private final MemberPackageRepository subscriptions;
    private final SubjectRepository subjects;
    private final UserRepository users;
    private final MemberRepository members;
    private final ActorPolicy actors;
    private final CoachCapacityService capacity;
    private final CoachWorkScheduleRepository schedules;
    private final ScheduleWriteLock mutex;
    private final SessionRepository sessions;
    private final InvoiceService payments;
    private final InvoiceRepository invoices;
    private final AuditService audit;
    private final Clock clock;

    @Transactional
    public AdvancedRegistrationResponse request(Integer subjectId, Integer coachId, String key, Authentication auth) {
        User member = actors.actor(auth);
        if (!ActorPolicy.role(member, "Member")) throw new AccessDeniedException("Member required");
        requireMember(member);
        requireKey(key);
        mutex.acquire(member.getCenterId());
        var existing = registrations.findByMemberIdAndRequestKey(member.getId(), key);
        if (existing.isPresent()) {
            var r = existing.get();
            if (!r.getSubject().getId().equals(subjectId) || !r.getCoach().getId().equals(coachId)) throw new BusinessConflictException("IDEMPOTENCY_CONFLICT: Key already has a different request");
            return AdvancedRegistrationResponse.from(r);
        }
        var subject = subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (!"Active".equalsIgnoreCase(subject.getStatus()) || subject.getAdvancedPrice() == null || subject.getAdvancedPrice().signum() <= 0)
            throw new BusinessException("SUBJECT_UNAVAILABLE: Active subject with a positive price required");
        User coach = users.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        capacity.requireCoach(coach, subject, member.getCenterId());
        capacity.requireCapacity(coach, subject);
        AdvancedRegistration r = new AdvancedRegistration();
        r.setMember(member); r.setCoach(coach); r.setSubject(subject); r.setRequestKey(key); r.setCreatedAt(LocalDateTime.now(clock));
        return AdvancedRegistrationResponse.from(registrations.save(r));
    }

    @Transactional
    public AdvancedRegistrationResponse decide(Integer id, boolean accept, String reason, Authentication auth) {
        User actor = actors.actor(auth);
        mutex.acquire(actor.getCenterId());
        AdvancedRegistration r = entity(id);
        ActorPolicy.center(actor, r.getMember().getCenterId());
        if (!actor.getId().equals(r.getCoach().getId())) { ActorPolicy.manager(actor); ActorPolicy.reason(reason); }
        if (!"Requested".equals(r.getStatus())) {
            if ((accept ? "Accepted" : "Rejected").equals(r.getStatus())) return AdvancedRegistrationResponse.from(r);
            throw new BusinessConflictException("REGISTRATION_STATE_CONFLICT: Registration already processed");
        }
        if (accept) { requireMember(r.getMember()); capacity.requireCapacity(r.getCoach(), r.getSubject()); }
        r.setStatus(accept ? "Accepted" : "Rejected"); r.setAcceptedBy(actor); r.setAcceptedAt(LocalDateTime.now(clock)); r.setReason(reason);
        audit.log(actor.getId(), "DECIDE_ADVANCED_REGISTRATION", "ADVANCED_REGISTRATION", id, r.getStatus() + "; reason=" + reason);
        return AdvancedRegistrationResponse.from(r);
    }

    @Transactional
    public SubscriptionOrderResponse order(Integer id, String method, String clientIp, Authentication auth) {
        User actor = actors.actor(auth);
        mutex.acquire(actor.getCenterId());
        AdvancedRegistration r = entity(id);
        if (!r.getMember().getId().equals(actor.getId())) throw new AccessDeniedException("Registration owner required");
        requireMember(actor);
        if (r.getSubscription() != null) {
            Invoice invoice = invoices.findBySubscriptionSubscriptionId(r.getSubscription().getSubscriptionId()).stream().findFirst().orElseThrow();
            if (!Objects.equals(invoice.getPaymentMethod(), method)) throw new BusinessConflictException("IDEMPOTENCY_CONFLICT: Payment method cannot change on retry");
            return response(r.getSubscription(), payments.existingOrder(invoice, clientIp));
        }
        if (!"Accepted".equals(r.getStatus())) throw new BusinessException("COACH_ACCEPTANCE_REQUIRED: Coach must accept before an invoice can be issued");
        capacity.requireCoach(r.getCoach(), r.getSubject(), actor.getCenterId());
        capacity.requireCapacity(r.getCoach(), r.getSubject());
        if (!"Active".equals(r.getSubject().getStatus())) throw new BusinessException("SUBJECT_UNAVAILABLE: Subject is inactive");
        MemberPackage sub = new MemberPackage(); sub.setMember(actor); sub.setSubject(r.getSubject()); sub.setPrimaryCoach(r.getCoach());
        sub.setPurchaseAmount(r.getSubject().getAdvancedPrice());
        sub.setRemainingCredit(0); sub.setValidityDays(45); sub.setStatus("Pending");
        subscriptions.save(sub); r.setSubscription(sub); r.setStatus("Invoiced");
        InvoiceOrderResponse order = payments.createAdvancedOrder(sub, method, clientIp);
        audit.log(actor.getId(), "CREATE_ADVANCED_ORDER", "ADVANCED_REGISTRATION", id, "Invoice=" + order.invoiceCode());
        return response(sub, order);
    }

    @Transactional(readOnly = true)
    public List<AdvancedRegistrationResponse> list(Authentication auth) {
        User actor = actors.actor(auth);
        return registrations.findByMemberCenterId(actor.getCenterId()).stream()
                .filter(r -> ActorPolicy.role(actor,"Member") ? r.getMember().getId().equals(actor.getId())
                        : ActorPolicy.role(actor,"Coach") ? r.getCoach().getId().equals(actor.getId())
                        : ActorPolicy.role(actor,"CenterManager") || ActorPolicy.role(actor,"Admin"))
                .map(AdvancedRegistrationResponse::from).toList();
    }

    public record WorkResponse(Integer coachId, boolean acceptingNew, Set<String> weeklySlots, Set<LocalDate> daysOff) { }
    @Transactional(readOnly = true)
    public WorkResponse work(Integer coachId,Authentication auth) {
        User actor=actors.actor(auth);
        User coach=users.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        ActorPolicy.center(actor,coach.getCenterId());
        var s=schedules.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach work schedule not found"));
        return new WorkResponse(coachId,s.isAcceptingNew(),Set.copyOf(s.getWeeklySlots()),Set.copyOf(s.getDaysOff()));
    }
    @Transactional(readOnly = true)
    public CoachCapacityService.Capacity capacity(Integer coachId,Integer subjectId,Authentication auth) {
        User actor=actors.actor(auth);
        User coach=users.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        ActorPolicy.center(actor,coach.getCenterId());
        return capacity.requireCapacity(coach,subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found")));
    }

    @Transactional
    public void configure(Integer coachId, Set<String> slots, Set<LocalDate> off, boolean accepting, Authentication auth) {
        User actor = actors.actor(auth); ActorPolicy.manager(actor); mutex.acquire(actor.getCenterId());
        User coach = users.findById(coachId).orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        ActorPolicy.center(actor, coach.getCenterId());
        if (!ActorPolicy.role(coach,"Coach") || slots == null || off == null || slots.stream().anyMatch(s -> !s.matches("(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY):[1-7]")))
            throw new BusinessException("INVALID_WORK_SCHEDULE: Supply weekday:slot keys and daysOff");
        CoachWorkSchedule schedule = schedules.findById(coachId).orElseGet(CoachWorkSchedule::new);
        schedule.setCoach(coach); schedule.setWeeklySlots(new HashSet<>(slots)); schedule.setDaysOff(new HashSet<>(off)); schedule.setAcceptingNew(accepting);
        for (Session session : sessions.findFiltered(null,coachId,null,LocalDate.now(clock),null,"Scheduled")) {
            if (!session.isPrivateSession()) continue;
            TimeSlot slot = Arrays.stream(TimeSlot.values()).filter(s -> s.getStartTime().equals(session.getStartTime())).findFirst().orElseThrow();
            if (!schedule.works(session.getSessionDate(),slot.getSlotNumber())) throw new BusinessConflictException("WORK_SCHEDULE_HAS_BOOKINGS: Resolve affected sessions first");
        }
        schedules.saveAndFlush(schedule);
        capacity.requireCommittedCapacity(coach);
        audit.log(actor.getId(), "CONFIGURE_COACH_WORK", "USER", coachId, "slots=" + slots + ";daysOff=" + off + ";acceptingNew=" + accepting);
    }

    private AdvancedRegistration entity(Integer id) { return registrations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Registration not found")); }
    private void requireMember(User member) {
        if (!ActorPolicy.role(member,"Member") || !"Active".equals(member.getStatus())) throw new BusinessException("MEMBER_NOT_ACTIVE: Active Member required");
        var profile = members.findById(member.getId()).orElseThrow(() -> new BusinessException("MEMBER_PROFILE_REQUIRED: Profile required"));
        if (profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) throw new BusinessException("BOOKING_BLOCKED: Member is blocked");
    }
    public static void requireKey(String key) { if (key == null || !key.matches("[A-Za-z0-9_.:-]{1,80}")) throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED: Supply a key of 1-80 safe characters"); }
    private SubscriptionOrderResponse response(MemberPackage sub, InvoiceOrderResponse i) {
        return new SubscriptionOrderResponse(sub.getSubscriptionId(),i.invoiceId(),i.invoiceCode(),i.amount(),i.paymentMethod(),i.status(),i.expiresAt(),i.paymentUrl());
    }
}
