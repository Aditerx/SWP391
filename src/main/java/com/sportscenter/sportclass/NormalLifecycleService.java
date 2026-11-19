package com.sportscenter.sportclass;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.*;
import com.sportscenter.common.exception.*;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.session.*;
import com.sportscenter.membership.CoachCapacityService;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.notification.*;
import com.sportscenter.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class NormalLifecycleService {
    private final SportsClassRepository classes;
    private final SessionRepository sessions;
    private final ClassEnrollmentRepository enrollments;
    private final InvoiceRepository invoices;
    private final ScheduleWriteLock mutex;
    private final CoachCapacityService coaches;
    private final SessionSlotValidator slots;
    private final HolidayRepository holidays;
    private final UserRepository users;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public SportsClassResponse confirm(Integer id, String reason, org.springframework.security.core.Authentication auth) {
        User actor = actors.actor(auth);
        requireOpeningActor(actor);
        SportsClass c = locked(id, actor);
        requireConfirmable(c,actor);
        if (c.getOpeningConfirmedAt() != null) return SportsClassResponse.from(c);
        LocalDateTime first = NormalClassPolicy.first(c,sessions), now = LocalDateTime.now(clock);
        if (!now.isBefore(first.minusHours(24))) ActorPolicy.reason(reason);
        c.setOpeningConfirmedBy(actor.getId()); c.setOpeningConfirmedAt(now); c.setOpeningReason(reason); c.setLifecycleVersion(c.getLifecycleVersion()+1);
        audit.log(actor.getId(),"CONFIRM_NORMAL_OPENING","CLASS",id,"late="+!now.isBefore(first.minusHours(24))+"; reason="+reason);
        notify(c,"Manager confirmed opening", "CONFIRMED:"+c.getLifecycleVersion());
        return SportsClassResponse.from(c);
    }
    private void requireOpeningActor(User actor) {
        if (actor==null || !"Active".equalsIgnoreCase(actor.getStatus()) || !ActorPolicy.role(actor,"CenterManager")
                || !com.sportscenter.training.SessionAttendancePolicy.permission(actor,"MANAGE_CLASSES"))
            throw new org.springframework.security.access.AccessDeniedException("OPENING_CONFIRMATION_FORBIDDEN: Active CenterManager with MANAGE_CLASSES required");
    }
    public void requireConfirmable(SportsClass c,User actor) {
        requireOpeningActor(actor);
        if (!java.util.Objects.equals(actor.getCenterId(),c.getCenterId()) || actor.getCenterId()==null)
            throw new org.springframework.security.access.AccessDeniedException("OPENING_CONFIRMATION_FORBIDDEN: Same-center CenterManager required");
        if(c.isChangePending()) throw new BusinessException("NORMAL_CHANGE_PENDING: Member choices must be resolved first");
        if(c.getOpeningConfirmedAt()!=null) return; // Confirmation retry remains idempotent.
        if (!c.isNormal() || !"Open".equals(c.getStatus()) || c.isPricingDraft()) throw new BusinessException("NORMAL_NOT_AWAITING_OPENING: Published Open Normal required");
        if (!LocalDateTime.now(clock).isBefore(NormalClassPolicy.first(c,sessions))) throw new BusinessException("NORMAL_OPENING_TOO_LATE: Decide before the first session");
        requireOpeningConditions(c);
    }

    SportsClass locked(Integer id, User actor) {
        Integer center = classes.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        ActorPolicy.center(actor,center); mutex.acquire(center);
        return classes.findByIdForUpdate(id).orElseThrow();
    }

    public long paidCount(SportsClass c) {
        return com.sportscenter.training.SessionAttendancePolicy.paidCount(c,enrollments,invoices);
    }

    public void requireOpeningConditions(SportsClass c) {
        if(c.getSubject()==null || !"Active".equalsIgnoreCase(c.getSubject().getStatus())) throw new BusinessException("SUBJECT_INACTIVE: Opening requires an active subject");
        if (c.getMinCapacity() == null) throw new BusinessException("NORMAL_MIN_CAPACITY_REQUIRED: Reconcile legacy minimum before confirmation");
        if (paidCount(c) < c.getMinCapacity()) throw new BusinessException("NORMAL_INSUFFICIENT_PAID: Paid Registered members are below minimum");
        List<Session> schedule = NormalClassPolicy.valid(c,sessions);
        if (schedule.size()!=12 || schedule.stream().anyMatch(s -> !"Scheduled".equals(s.getStatus()))) throw new BusinessException("CLASS_NOT_READY: Twelve scheduled valid sessions required");
        var members=enrollments.findByClassIdWithDetails(c.getId()).stream().filter(e -> "Registered".equals(e.getStatus())).map(e -> e.getMember().getId()).toList();
        for (Session s : schedule) {
            User coach = s.getCoach()==null ? c.getCoach() : s.getCoach();
            if (coach == null) throw new BusinessException("COACH_REQUIRED: Coach required");
            coaches.requireCoach(coach,c.getSubject(),c.getCenterId()); coaches.requireWorking(coach.getId(),s.getSessionDate(),s.getStartTime());
            if (holidays.existsByHolidayDateAndIsActiveTrue(s.getSessionDate())) throw new BusinessException("HOLIDAY_BLOCKED: Center is closed");
            var room=s.getRoom();
            if (room==null || !Objects.equals(room.getCenterId(),c.getCenterId()) || !"Available".equals(room.getStatus()) || room.getCapacity()<c.getMaxCapacity())
                throw new BusinessException("ROOM_UNAVAILABLE: Suitable same-center room required");
            com.sportscenter.room.RoomActivity.require(room,c.getSubject()); slots.validateSession(c,room,s.getStartTime(),s.getEndTime());
            if (!sessions.findCoachConflicts(coach.getId(),s.getSessionDate(),s.getStartTime(),s.getEndTime(),s.getId()).isEmpty()
                || !sessions.findRoomConflicts(room.getId(),s.getSessionDate(),s.getStartTime(),s.getEndTime(),s.getId()).isEmpty())
                throw new BusinessConflictException("NORMAL_RESOURCE_CONFLICT: Opening schedule conflicts");
            for(Integer member:members) {
                if(!sessions.findMemberConflicts(member,s.getSessionDate(),s.getStartTime(),s.getEndTime(),s.getId()).isEmpty())
                    throw new BusinessConflictException("SCHEDULE_CONFLICT: A registered Member has another session");
            }
        }
    }

    @Transactional
    public void reconcile(Integer id) {
        Integer center=classes.findCenterIdById(id).orElse(null); if(center==null)return;
        mutex.acquire(center); SportsClass c=classes.findByIdForUpdate(id).orElseThrow();
        if (!c.isNormal() || c.isPricingDraft() || !"Open".equals(c.getStatus())) return;
        if(c.isChangePending()) { warn(c,"CHOICES_PENDING","Member choices are pending; no automatic start"); return; }
        LocalDateTime first;
        try { first=NormalClassPolicy.first(c,sessions); } catch(BusinessException invalid) { return; }
        LocalDateTime now=LocalDateTime.now(clock);
        if (!now.isBefore(first) && c.getOpeningConfirmedAt()!=null) {
            try { requireOpeningConditions(c); }
            catch(BusinessException invalid) { warn(c,"BLOCKED",invalid.getMessage()); return; }
            c.setStatus("Ongoing"); c.setLifecycleVersion(c.getLifecycleVersion()+1);
            audit.log(null,"START_NORMAL_CLASS","CLASS",id,"Confirmed opening started"); notify(c,"Class is now Ongoing","STARTED");
        } else if (!now.isBefore(first.minusHours(24)) && c.getOpeningConfirmedAt()==null) warn(c,"UNCONFIRMED","Manager opening decision is overdue; no automatic start/cancellation");
        else if (!now.isBefore(first.minusHours(48))) warn(c,"CUTOFF","Registration closed; paid="+paidCount(c)+"; minimum="+c.getMinCapacity());
    }

    private void warn(SportsClass c,String phase,String message) {
        // Notification dedupe survives restarts; event delivery is AFTER_COMMIT.
        List<Integer> managers=users.findAll().stream().filter(u -> Objects.equals(u.getCenterId(),c.getCenterId()) && ActorPolicy.role(u,"CenterManager") && "Active".equals(u.getStatus())).map(User::getId).toList();
        events.publishEvent(NotificationTemplates.event(NotificationType.CENTER_INCIDENT_UPDATED,managers,message,"CLASS",c.getId(),"NORMAL:"+c.getId()+":"+c.getLifecycleVersion()+":"+phase));
    }
    private void notify(SportsClass c,String message,String key) {
        var ids=enrollments.findByClassIdWithDetails(c.getId()).stream().filter(e -> "Registered".equals(e.getStatus())).map(e -> e.getMember().getId()).toList();
        events.publishEvent(NotificationTemplates.event(NotificationType.CENTER_INCIDENT_UPDATED,ids,message,"CLASS",c.getId(),"NORMAL:"+c.getId()+":"+key));
    }
}
