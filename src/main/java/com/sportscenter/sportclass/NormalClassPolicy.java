package com.sportscenter.sportclass;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import java.time.*;
import java.util.*;

/** Shared by admission, cashier, callbacks and lifecycle; persisted sessions are the time source. */
public final class NormalClassPolicy {
    private NormalClassPolicy() {}
    public static int minimum(int maximum) { return (2 * maximum + 2) / 3; }
    public static int validateMinimum(Integer maximum, Integer chosen) {
        if (maximum == null || maximum < 1 || maximum > 15)
            throw new BusinessException("NORMAL_CAPACITY_INVALID: Normal capacity must be in 1..15");
        int value = chosen == null ? minimum(maximum) : chosen;
        if (value < minimum(maximum) || value > maximum)
            throw new BusinessException("NORMAL_MIN_CAPACITY_INVALID: Minimum must be between ceil(2*max/3) and max");
        return value;
    }
    public static List<Session> valid(SportsClass c, SessionRepository sessions) {
        var valid=sessions.findBySportsClassId(c.getId()).stream()
            .filter(s -> !"Cancelled".equals(s.getStatus()) && !s.isCenterInvalidated()).toList();
        if(valid.stream().anyMatch(s -> s.getSessionDate()==null || s.getStartTime()==null || s.getEndTime()==null))
            throw new BusinessException("CLASS_NOT_READY: Reconcile missing Normal session times");
        return valid.stream()
            .sorted(Comparator.comparing((Session s) -> s.getSessionDate().atTime(s.getStartTime()))).toList();
    }
    public static LocalDateTime first(SportsClass c, SessionRepository sessions) {
        return valid(c, sessions).stream().findFirst()
            .map(s -> s.getSessionDate().atTime(s.getStartTime()))
            .orElseThrow(() -> new BusinessException("CLASS_NOT_READY: No valid Normal schedule"));
    }
    public static LocalDateTime cutoff(SportsClass c, SessionRepository sessions) { return first(c, sessions).minusHours(48); }
    public static void requireAdmission(SportsClass c, SessionRepository sessions, Clock clock) {
        if (!c.isNormal()) return;
        if (c.isChangePending()) throw new BusinessException("NORMAL_CHANGE_PENDING: Resolve the proposed change before new admission");
        if (!"Open".equals(c.getStatus()) || c.isPricingDraft())
            throw new BusinessException("CLASS_NOT_OPEN: Normal registration requires a published Open class");
        if (!LocalDateTime.now(clock).isBefore(cutoff(c, sessions)))
            throw new BusinessException("NORMAL_REGISTRATION_CLOSED: Registration closes 48 hours before the first session");
    }
}
