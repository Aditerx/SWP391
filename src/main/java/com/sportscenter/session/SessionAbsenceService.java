package com.sportscenter.session;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.audit.AuditService;
import com.sportscenter.user.User;
import com.sportscenter.training.Attendance;
import com.sportscenter.training.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class SessionAbsenceService {
    private final SessionRepository sessions;
    private final SessionAbsenceNoticeRepository notices;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final Clock clock;
    private final AttendanceRepository attendances;
    public record Notice(Integer id,Integer sessionId,Integer memberId,LocalDateTime reportedAt,String reason,boolean active,LocalDateTime supersededAt) {
        static Notice from(SessionAbsenceNotice n){return new Notice(n.getId(),n.getSession().getId(),n.getMemberId(),n.getReportedAt(),n.getReason(),n.isActive(),n.getSupersededAt());}
    }
    @Transactional
    public Notice report(Integer id,String reason,Authentication auth) {
        User member=actors.actor(auth);
        mutex.acquire(member.getCenterId());
        Session s=sessions.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ActorPolicy.center(member,s.getSportsClass().getCenterId());
        if(!s.isPrivateSession()) throw new BusinessException("NORMAL_CLASS_NON_CANCELLABLE: Normal does not support absence notices");
        if(!ActorPolicy.role(member,"Member") || s.getBookedMember()==null || !s.getBookedMember().getId().equals(member.getId()))
            throw new AccessDeniedException("Only the booked Member may report absence");
        Duration notice=Duration.between(LocalDateTime.now(clock),s.getSessionDate().atTime(s.getStartTime()));
        if(!"Scheduled".equals(s.getStatus()) || s.isCenterInvalidated() || s.isAttendanceFinalized() || notice.isNegative() || notice.isZero() || notice.compareTo(Duration.ofHours(12))>=0)
            throw new BusinessException("ABSENCE_WINDOW_CLOSED: Absence notice requires 0 < time to session < 12 hours");
        ActorPolicy.reason(reason);
        var previous=notices.findFirstBySessionIdAndActiveTrue(id);
        if(previous.isPresent()) {
            recordAbsence(s,member,previous.get());
            return Notice.from(previous.get());
        }
        SessionAbsenceNotice n=new SessionAbsenceNotice();n.setSession(s);n.setMemberId(member.getId());n.setReason(reason);n.setReportedAt(LocalDateTime.now(clock));
        n.setOriginalStart(s.getSessionDate().atTime(s.getStartTime()));n.setOriginalEnd(s.getSessionDate().atTime(s.getEndTime()));notices.save(n);
        recordAbsence(s,member,n);
        audit.log(member.getId(),"REPORT_SESSION_ABSENCE","SESSION",id,reason);
        return Notice.from(n);
    }
    @Transactional(readOnly=true)
    public List<Notice> history(Integer id,Authentication auth) {
        User actor=actors.actor(auth);Session s=sessions.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ActorPolicy.center(actor,s.getSportsClass().getCenterId());
        Integer assigned=s.getCoach()!=null?s.getCoach().getId():s.getSportsClass().getCoach()==null?null:s.getSportsClass().getCoach().getId();
        if(!(s.getBookedMember()!=null && actor.getId().equals(s.getBookedMember().getId())) && !actor.getId().equals(assigned)) ActorPolicy.manager(actor);
        return notices.findBySessionIdOrderByIdAsc(id).stream().map(Notice::from).toList();
    }
    public void supersedeForTimeChange(Session s) {
        notices.findFirstBySessionIdAndActiveTrue(s.getId()).ifPresent(n->{
            LocalDateTime now=LocalDateTime.now(clock);
            n.setActive(false);n.setSupersededAt(now);
            attendances.findBySessionIdAndMemberId(s.getId(),n.getMemberId())
                    .filter(a->Objects.equals(a.getAbsenceNoticeId(),n.getId()))
                    .filter(a->"Absent".equals(a.getState()) && a.getRecordedBy()!=null && Objects.equals(a.getRecordedBy().getId(),n.getMemberId()))
                    .ifPresent(a->{a.setSupersededAt(now);attendances.save(a);
                        audit.log(null,"SUPERSEDE_ABSENCE_ATTENDANCE","attendances",a.getAttendanceId(),"Notice "+n.getId()+" superseded by session time change; original attendance retained");});
        });
    }

    private void recordAbsence(Session session,User member,SessionAbsenceNotice notice) {
        if(attendances.findBySessionIdAndMemberId(session.getId(),member.getId()).isPresent())return;
        Attendance attendance=new Attendance();
        attendance.setSession(session);attendance.setMember(member);attendance.setRecordedBy(member);
        attendance.setState("Absent");attendance.setNote(notice.getReason());attendance.setAbsenceNoticeId(notice.getId());
        attendances.save(attendance);
    }
}
