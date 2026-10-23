package com.sportscenter.session;

import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.common.exception.BusinessException;
import java.time.*;

/** Readiness is based on the actual persisted sessions, not a client-supplied class date. */
public final class NormalSchedulePolicy {
    private NormalSchedulePolicy() {}
    public static void requireReady(SportsClass c, SessionRepository repository, Clock clock) {
        var sessions = repository.findFiltered(c.getId(), null, null, null, null, null).stream()
                .filter(s -> !"Cancelled".equals(s.getStatus())).toList();
        if (sessions.size() != 12 || sessions.stream().anyMatch(s -> !"Scheduled".equals(s.getStatus())
                || s.getSessionDate() == null || s.getStartTime() == null || s.getEndTime() == null
                || !LocalDateTime.of(s.getSessionDate(), s.getStartTime()).isAfter(LocalDateTime.now(clock))
                || TimeSlot.findByTime(s.getStartTime(), s.getEndTime()).isEmpty()
                || s.getRoom() == null || !"Available".equals(s.getRoom().getStatus()))) {
            throw new BusinessException("CLASS_NOT_READY: Normal registration requires 12 valid future sessions");
        }
    }
}
