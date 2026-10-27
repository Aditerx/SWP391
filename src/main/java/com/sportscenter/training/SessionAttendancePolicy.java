package com.sportscenter.training;
import com.sportscenter.session.Session;
import com.sportscenter.common.exception.BusinessException;
import java.time.LocalDateTime;
public final class SessionAttendancePolicy {
    private SessionAttendancePolicy(){ }
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
