package com.sportscenter.session;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.training.SessionAttendancePolicy;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import java.time.Clock;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class SessionActionPolicy {
    private final com.sportscenter.training.AttendanceRepository attendances;
    private final com.sportscenter.enrollment.ClassEnrollmentRepository enrollments;
    private final com.sportscenter.invoice.InvoiceRepository invoices;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Value("${scms.early-attendance.enabled:false}") private boolean earlyAttendanceEnabled;

    public static String code(RuntimeException error, String fallback) {
        String message=error.getMessage(); int colon=message==null?-1:message.indexOf(':');
        return colon>0 && message.substring(0,colon).matches("[A-Z_]+") ? message.substring(0,colon) : fallback;
    }
    public static void requireRecorder(User actor, Session session) {
        if (actor==null || !"Active".equalsIgnoreCase(actor.getStatus()) || actor.getRole()==null
                || actor.getCenterId()==null || !java.util.Objects.equals(actor.getCenterId(),session.getSportsClass().getCenterId())
                || !(SessionAttendancePolicy.permission(actor,"RECORD_RESULT") || SessionAttendancePolicy.permission(actor,"MANAGE_CLASSES")
                    || SessionAttendancePolicy.permission(actor,"REGISTER_MEMBER") || SessionAttendancePolicy.permission(actor,"MANAGE_USERS"))
                || ActorPolicy.role(actor,"Member"))
            throw new AccessDeniedException("ATTENDANCE_FORBIDDEN: Active session staff with attendance permission in the same center required");
        if (ActorPolicy.role(actor,"Coach") && !actor.getId().equals(SessionAttendancePolicy.coachId(session)))
            throw new BusinessException("NOT_YOUR_CLASS: Coach is not assigned to this session");
    }
    public SessionResponse.Actions metadata(Session session, User actor) {
        LocalDateTime now=SessionAttendancePolicy.now(clock); String attendance=null,cancellation=null;
        try {
            requireRecorder(actor,session);
            SessionAttendancePolicy.requireWindow(session,now,earlyAttendanceEnabled);
            if(SessionAttendancePolicy.coachId(session)==null) throw new BusinessException("COACH_REQUIRED: Assigned Coach required");
            if (session.getBookedMember()!=null)
                SessionAttendancePolicy.requireEligibility(session,session.getBookedMember(),enrollments,invoices,now);
            else {
                long paid=SessionAttendancePolicy.paidCount(session.getSportsClass(),enrollments,invoices);
                if (paid==0) throw new BusinessException("NOT_YOUR_CLASS: No paid Registered Member for this session");
                if (now.isBefore(session.getSessionDate().atTime(session.getStartTime()))
                        && session.getSportsClass().getMinCapacity()!=null && paid<session.getSportsClass().getMinCapacity())
                    throw new BusinessException("NORMAL_INSUFFICIENT_PAID: Paid Registered members are below minimum");
            }
        } catch (BusinessException | AccessDeniedException error) { attendance=code(error,"ATTENDANCE_FORBIDDEN"); }
        try { SessionCancellationPolicy.require(session,actor,now,attended(session)); }
        catch (BusinessException | AccessDeniedException error) { cancellation=code(error,"CANCELLATION_FORBIDDEN"); }
        var deadline=!session.isPrivateSession() || session.getSessionDate()==null || session.getStartTime()==null ? null
                : session.getSessionDate().atTime(session.getStartTime()).minusHours(12).atZone(SessionAttendancePolicy.ZONE).toOffsetDateTime();
        return new SessionResponse.Actions(attendance==null,attendance,cancellation==null,deadline,cancellation);
    }
    public boolean attended(Session session) {
        return session.getBookedMember()!=null && attendances.findBySessionIdAndMemberId(session.getId(),session.getBookedMember().getId())
                .filter(a -> "Present".equals(a.getState()) || "Absent".equals(a.getState())).isPresent();
    }
}
