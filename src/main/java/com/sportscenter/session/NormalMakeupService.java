package com.sportscenter.session;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.audit.AuditService;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.user.UserRepository;
import com.sportscenter.membership.CoachCapacityService;
import com.sportscenter.notification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class NormalMakeupService {
    private final CenterIncidentRepository incidents;
    private final SessionRepository sessions;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final ClassEnrollmentRepository enrollments;
    private final HolidayRepository holidays;
    private final SessionSlotValidator slots;
    private final CoachCapacityService capacity;
    private final ActorPolicy actors;
    private final ScheduleWriteLock mutex;
    private final AuditService audit;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalMakeupRequest")
    public record Request(@jakarta.validation.constraints.NotNull LocalDate date,@jakarta.validation.constraints.NotNull LocalTime startTime,
            @jakarta.validation.constraints.NotNull LocalTime endTime,@jakarta.validation.constraints.NotNull Integer roomId,
            @jakarta.validation.constraints.NotNull Integer coachId,@jakarta.validation.constraints.NotBlank String reason) { }
    @Transactional
    public SessionResponse create(Integer incidentId,Request r,Authentication auth){
        var actor=actors.actor(auth);ActorPolicy.manager(actor);ActorPolicy.reason(r.reason());mutex.acquire(actor.getCenterId());
        CenterIncident incident=incidents.findById(incidentId).orElseThrow(()->new ResourceNotFoundException("Incident not found"));
        Session old=sessions.findByIdForUpdate(incident.getSession().getId()).orElseThrow();ActorPolicy.center(actor,old.getSportsClass().getCenterId());
        if(old.isPrivateSession()||!old.isCenterInvalidated()||!incident.isSessionCompensated())throw new BusinessException("NORMAL_MAKEUP_EVIDENCE_REQUIRED: Verify that the original Normal session was not held");
        var existing=sessions.findByMakeupForId(old.getId());
        if(existing.isPresent()){
            Session s=existing.get();
            if(!Objects.equals(r.date(),s.getSessionDate())||!Objects.equals(r.startTime(),s.getStartTime())||!Objects.equals(r.endTime(),s.getEndTime())||!Objects.equals(r.roomId(),s.getRoom().getId())||!Objects.equals(r.coachId(),s.getCoach().getId()))
                throw new BusinessConflictException("MAKEUP_ALREADY_CREATED: Use the existing linked session");
            return SessionResponse.from(s,incident.getAffectedMembers().size());
        }
        if(r.date()==null||r.startTime()==null||r.endTime()==null||!r.date().atTime(r.startTime()).isAfter(LocalDateTime.now(clock)))throw new BusinessException("MAKEUP_TIME_INVALID: Makeup must be a future standard session");
        if(holidays.existsByHolidayDateAndIsActiveTrue(r.date()))throw new BusinessException("HOLIDAY_BLOCKED: Center is closed");
        var room=rooms.findById(r.roomId()).orElseThrow(()->new ResourceNotFoundException("Room not found"));
        var coach=users.findById(r.coachId()).orElseThrow(()->new ResourceNotFoundException("Coach not found"));
        ActorPolicy.center(actor,room.getCenterId());capacity.requireCoach(coach,old.getSportsClass().getSubject(),actor.getCenterId());
        slots.validateSession(old.getSportsClass(),room,r.startTime(),r.endTime());
        if(!"Available".equals(room.getStatus())||room.getCapacity()==null||room.getCapacity()<old.getSportsClass().getMaxCapacity())throw new BusinessException("ROOM_UNAVAILABLE: Suitable room capacity required");
        if(!sessions.findCoachConflicts(coach.getId(),r.date(),r.startTime(),r.endTime(),null).isEmpty()||!sessions.findRoomConflicts(room.getId(),r.date(),r.startTime(),r.endTime(),null).isEmpty())
            throw new BusinessConflictException("MAKEUP_RESOURCE_CONFLICT: Coach or room is occupied");
        for(Integer member:incident.getAffectedMembers())if(!sessions.findMemberConflicts(member,r.date(),r.startTime(),r.endTime(),null).isEmpty())throw new BusinessConflictException("MEMBER_CONFLICT: An affected Member has another session");
        Session makeup=new Session();makeup.setSportsClass(old.getSportsClass());makeup.setRoom(room);makeup.setCoach(coach);makeup.setSessionDate(r.date());makeup.setStartTime(r.startTime());makeup.setEndTime(r.endTime());makeup.setMakeupFor(old);makeup.setCreatedBy(actor.getId());makeup.setSupportReason(r.reason());sessions.save(makeup);
        var c=old.getSportsClass();if(c.getEndDate()==null||r.date().isAfter(c.getEndDate()))c.setEndDate(r.date());
        if("Completed".equals(c.getStatus()))c.setStatus("Ongoing");
        enrollments.findByClassIdWithDetails(c.getId()).stream().filter(e->incident.getAffectedMembers().contains(e.getMember().getId())&&"Completed".equals(e.getStatus())).forEach(e->e.setStatus("Registered"));
        audit.log(actor.getId(),"CREATE_LINKED_NORMAL_MAKEUP","SESSION",makeup.getId(),"original="+old.getId()+"; incident="+incidentId+"; reason="+r.reason());
        events.publishEvent(NotificationTemplates.event(NotificationType.SESSION_RESCHEDULED,List.copyOf(incident.getAffectedMembers()),"Buổi bù của lớp "+c.getName()+" đã được xếp: "+r.date()+" "+r.startTime(),"SESSION",makeup.getId(),"NORMAL_MAKEUP:"+old.getId()));
        return SessionResponse.from(makeup,incident.getAffectedMembers().size());
    }
}
