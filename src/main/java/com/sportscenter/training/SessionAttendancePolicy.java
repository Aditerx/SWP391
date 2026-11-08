package com.sportscenter.training;
import com.sportscenter.session.Session;
import com.sportscenter.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import com.sportscenter.user.User;
import com.sportscenter.common.ActorPolicy;
import org.springframework.security.access.AccessDeniedException;
public final class SessionAttendancePolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private SessionAttendancePolicy(){ }
    public static LocalDateTime now(Clock clock) { return LocalDateTime.ofInstant(clock.instant(), ZONE); }
    public static LocalDateTime cutoff(Session session) { return session.getSessionDate().plusDays(1).atStartOfDay(); }
    public static Integer coachId(Session s) {
        return s.getCoach() != null ? s.getCoach().getId()
                : s.getSportsClass() != null && s.getSportsClass().getCoach() != null ? s.getSportsClass().getCoach().getId() : null;
    }
    public static boolean permission(User actor, String name) {
        return actor != null && actor.getRole() != null && actor.getRole().getPermissions().stream().anyMatch(p -> name.equals(p.getName()));
    }
    public static boolean correctionActor(User actor) {
        return actor != null && "Active".equalsIgnoreCase(actor.getStatus())
                && (ActorPolicy.role(actor, "Coach") ? permission(actor, "RECORD_RESULT")
                : (ActorPolicy.role(actor, "CenterManager") || ActorPolicy.role(actor, "Admin")) && permission(actor, "MANAGE_CLASSES"));
    }
    public static boolean correctionScope(User actor, Session s) {
        return correctionActor(actor) && s != null && s.getSportsClass() != null && actor.getCenterId() != null
                && actor.getCenterId().equals(s.getSportsClass().getCenterId())
                && (!ActorPolicy.role(actor, "Coach") || actor.getId().equals(coachId(s)));
    }
    public static AccessDeniedException forbidden() {
        return new AccessDeniedException("ATTENDANCE_CORRECTION_FORBIDDEN: Bạn không có quyền hoặc không được phân công phụ trách buổi học này nên không thể sửa điểm danh hoặc ghi chú.");
    }
    public static void requireCorrection(User actor, Session s, LocalDateTime now) {
        if (!correctionScope(actor, s)) throw forbidden();
        if (s.getSessionDate() == null || s.getEndTime() == null) throw new BusinessException("SESSION_TIME_REQUIRED: Reconcile the legacy session time");
        if (s.isCenterInvalidated() || !("Scheduled".equals(s.getStatus()) || "Completed".equals(s.getStatus())))
            throw new BusinessException("SESSION_NOT_HELD: Preserve incident and attendance history");
        if (now.isBefore(s.getSessionDate().atTime(s.getEndTime())))
            throw new BusinessException("USE_SESSION_ATTENDANCE: Record in-session attendance before session end");
        if (ActorPolicy.role(actor, "Coach") && !now.isBefore(cutoff(s)))
            throw new AccessDeniedException("ATTENDANCE_CORRECTION_DEADLINE_PASSED: Đã hết hạn sửa điểm danh và ghi chú của Coach. Vui lòng liên hệ quản lý trung tâm.");
    }
    public record Metadata(boolean correctionAllowed, OffsetDateTime correctionDeadline, OffsetDateTime serverTime) { }
    public static Metadata metadata(Session s, User actor, Clock clock) {
        var now = now(clock);
        boolean allowed = false;
        if (s != null) {
            try { requireCorrection(actor, s, now); allowed = true; }
            catch (BusinessException | AccessDeniedException ignored) { /* Read metadata never grants mutation rights. */ }
        }
        boolean manager = actor != null && (ActorPolicy.role(actor, "CenterManager") || ActorPolicy.role(actor, "Admin"));
        return new Metadata(allowed, s == null || s.getSessionDate() == null || manager ? null : cutoff(s).atZone(ZONE).toOffsetDateTime(),
                now.atZone(ZONE).toOffsetDateTime());
    }
    public static String note(String note) {
        if (note == null) return null;
        String value = note.strip();
        if (value.codePointCount(0, value.length()) > 1000) throw new BusinessException("NOTE_TOO_LONG: Note must contain at most 1000 characters after trim");
        return value.isEmpty() ? null : value;
    }
    public static String reason(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("REASON_REQUIRED: Supply a correction reason");
        String value = reason.strip();
        if (value.codePointCount(0, value.length()) > 1000) throw new BusinessException("REASON_TOO_LONG: Reason must contain at most 1000 characters after trim");
        return value;
    }
    public static String noteAudit(String before, String after) {
        return " | Note: " + com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("before", before).put("after", after);
    }
    public static void requireWindow(Session s,LocalDateTime now){
        if(s.getSessionDate()==null||s.getStartTime()==null||s.getEndTime()==null)throw new BusinessException("SESSION_TIME_REQUIRED: Reconcile the legacy session time");
        if(!"Scheduled".equals(s.getStatus())||s.isAttendanceFinalized()||s.isCenterInvalidated()||now.isBefore(s.getSessionDate().atTime(s.getStartTime()))||!now.isBefore(s.getSessionDate().atTime(s.getEndTime())))
            throw new BusinessException("ATTENDANCE_WINDOW_CLOSED: Attendance is allowed from session start until before session end");
    }
    public static String state(String state){
        if("Present".equalsIgnoreCase(state))return "Present";
        if("Absent".equalsIgnoreCase(state))return "Absent";
        throw new BusinessException("INVALID_ATTENDANCE_STATE: New attendance accepts only Present or Absent");
    }
}
