package com.sportscenter.membership;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.session.*;
import com.sportscenter.subject.Subject;
import com.sportscenter.user.User;
import com.sportscenter.specialization.CoachSpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CoachCapacityService {
    private final CoachWorkScheduleRepository schedules;
    private final CoachSpecializationRepository specializations;
    private final MemberPackageRepository subscriptions;
    private final InvoiceRepository invoices;
    private final HolidayRepository holidays;
    private final RoomRepository rooms;
    private final SessionRepository sessions;
    private final EntitlementTimePolicy timePolicy;
    private final Clock clock;

    public void requireCoach(User coach, Subject subject, Integer centerId) {
        if (!"Active".equalsIgnoreCase(coach.getStatus()) || coach.getRole() == null || !"Coach".equalsIgnoreCase(coach.getRole().getName())
                || !Objects.equals(centerId, coach.getCenterId())) throw new BusinessException("COACH_UNAVAILABLE: Active Coach in the same center required");
        boolean qualified = specializations.findByCoachUserId(coach.getId()).stream().anyMatch(a -> a.getSpecialization() != null
                && "Active".equalsIgnoreCase(a.getSpecialization().getStatus()) && a.getSpecialization().getSubject() != null
                && Objects.equals(subject.getId(), a.getSpecialization().getSubject().getId()));
        if (!qualified) throw new BusinessException("COACH_SUBJECT_MISMATCH: Coach is not qualified for this subject");
    }

    public void requireWorking(Integer coachId, LocalDate date, java.time.LocalTime start) {
        TimeSlot slot = Arrays.stream(TimeSlot.values()).filter(s -> s.getStartTime().equals(start)).findFirst()
                .orElseThrow(() -> new BusinessException("INVALID_SLOT: Standard slot required"));
        if (!schedules.findById(coachId).map(s -> s.works(date, slot.getSlotNumber())).orElse(false))
            throw new BusinessException("COACH_NOT_WORKING: Slot is outside the published work schedule");
    }

    private record Demand(Subject subject, LocalDateTime deadline, int credits) { }
    private record Candidate(LocalDate date, TimeSlot slot) { }
    public record Capacity(int availableSlots, int outstandingCredits, int requestedCredits) { }

    /** One coach slot is counted once even when several rooms or subjects can use it. */
    @Transactional(readOnly = true)
    public Capacity requireCapacity(User coach, Subject subject) {
        return evaluate(coach,subject,true);
    }
    @Transactional(readOnly=true)
    public Capacity requireCommittedCapacity(User coach){return evaluate(coach,null,false);}
    @Transactional(readOnly=true)
    public String activationProblem(User coach){
        try{evaluate(coach,null,false);return null;}
        catch(BusinessException problem){return problem.getMessage();}
    }
    private Capacity evaluate(User coach,Subject subject,boolean incoming) {
        if(incoming)requireCoach(coach, subject, coach.getCenterId());
        CoachWorkSchedule schedule = schedules.findById(coach.getId()).orElseThrow(() -> new BusinessException("COACH_SCHEDULE_REQUIRED: Publish a work schedule first"));
        if (incoming&&!schedule.isAcceptingNew()) throw new BusinessException("COACH_NOT_ACCEPTING: Coach is not accepting new registrations");
        LocalDateTime now = LocalDateTime.now(clock);
        List<Demand> demands = new ArrayList<>();
        for (MemberPackage sub : subscriptions.findByPrimaryCoachId(coach.getId())) {
            if (sub.getSubject() == null) throw new BusinessException("ENTITLEMENT_RECONCILIATION_REQUIRED: Legacy subject mapping is missing");
            if ("Active".equals(sub.getStatus()) && sub.getExpiresAt() != null && sub.getExpiresAt().isAfter(now)) {
                demands.add(new Demand(sub.getSubject(), sub.getExpiresAt(), Math.max(0, sub.getRemainingCredit()==null?0:sub.getRemainingCredit())));
            } else if ("Pending".equals(sub.getStatus())) {
                boolean live = invoices.findBySubscriptionSubscriptionId(sub.getSubscriptionId()).stream().anyMatch(i -> "Pending".equals(i.getPaymentStatus())
                        && i.getExpiresAt() != null && i.getExpiresAt().isAfter(now));
                if (live) demands.add(new Demand(sub.getSubject(), EntitlementTimePolicy.deadline(now, 0, timePolicy.intervals(sub)), 12));
            }
        }
        int outstanding = demands.stream().mapToInt(Demand::credits).sum();
        MemberPackage proposed = new MemberPackage();
        LocalDateTime targetDeadline = EntitlementTimePolicy.deadline(now, 0, timePolicy.intervals(proposed));
        if(incoming)demands.add(new Demand(subject, targetDeadline, 12));
        if(demands.isEmpty())return new Capacity(0,0,0);
        demands.sort(Comparator.comparing(Demand::deadline));
        LocalDateTime horizon = demands.stream().map(Demand::deadline).max(Comparator.naturalOrder()).orElse(targetDeadline);
        var closedDates = holidays.findByIsActiveTrue().stream().map(h -> h.getHolidayDate()).collect(java.util.stream.Collectors.toSet());
        var compatibleRooms = rooms.findAll().stream().filter(r -> Objects.equals(r.getCenterId(), coach.getCenterId()) && "Available".equals(r.getStatus())).toList();
        var busy = sessions.findFiltered(null,null,null,now.toLocalDate(),horizon.toLocalDate(),null).stream()
                .filter(s -> !"Cancelled".equals(s.getStatus())).toList();
        List<Candidate> available = new ArrayList<>();
        for (LocalDate date = now.toLocalDate(); !date.isAfter(horizon.toLocalDate()); date = date.plusDays(1)) {
            if (closedDates.contains(date)) continue;
            for (TimeSlot slot : TimeSlot.values()) {
                if (!schedule.works(date, slot.getSlotNumber()) || date.atTime(slot.getStartTime()).isBefore(now.plusHours(12))) continue;
                LocalDate candidateDate=date;
                boolean coachBusy=busy.stream().anyMatch(s -> overlaps(s,candidateDate,slot) && Objects.equals(coach.getId(),s.getCoach()!=null?s.getCoach().getId():s.getSportsClass().getCoach()==null?null:s.getSportsClass().getCoach().getId()));
                if (!coachBusy) available.add(new Candidate(date, slot));
            }
        }
        int slots = available.size();
        // Conservative earliest-deadline allocation checks all sold but unbooked credits across subjects.
        for (Demand demand : demands) {
            int remaining = demand.credits();
            Iterator<Candidate> iterator = available.iterator();
            while (remaining > 0 && iterator.hasNext()) {
                Candidate c = iterator.next();
                if (c.date().atTime(c.slot().getEndTime()).isAfter(demand.deadline()) || demand.subject().isOutdoor() && !c.slot().isOutdoorAllowed()) continue;
                boolean roomFree = compatibleRooms.stream().anyMatch(r -> r.isOutdoor() == demand.subject().isOutdoor()
                        && busy.stream().noneMatch(s -> Objects.equals(r.getId(),s.getRoom().getId()) && overlaps(s,c.date(),c.slot())));
                if (roomFree) { iterator.remove(); remaining--; }
            }
            if (remaining > 0) throw new BusinessException("COACH_CAPACITY_INSUFFICIENT: Published coach/room capacity cannot cover outstanding credits and this registration");
        }
        return new Capacity(slots, outstanding, incoming?12:0);
    }
    private boolean overlaps(Session session,LocalDate date,TimeSlot slot) {
        return session.getSessionDate().equals(date) && session.getStartTime().isBefore(slot.getEndTime()) && session.getEndTime().isAfter(slot.getStartTime());
    }
}
