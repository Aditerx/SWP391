package com.sportscenter.membership;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.holiday.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class EntitlementTimePolicy {
    private final HolidayRepository holidays;
    private final SubscriptionPauseRepository pauses;

    public record Interval(LocalDateTime start, LocalDateTime end) { }

    /** Consume 45 days of open time, merging all closure sources before counting. */
    public static LocalDateTime deadline(LocalDateTime paidAt, long extraSeconds, Collection<Interval> closures) {
        return deadline(paidAt,Duration.ofSeconds(extraSeconds),closures);
    }
    public static LocalDateTime deadline(LocalDateTime paidAt,Duration extra,Collection<Interval> closures) {
        if (paidAt == null) throw new BusinessException("ENTITLEMENT_RECONCILIATION_REQUIRED: Missing payment timestamp");
        Duration remaining = Duration.ofDays(45).plus(extra);
        if (remaining.isNegative()) throw new BusinessException("INVALID_EXTENSION: Negative entitlement duration");
        List<Interval> merged = merged(closures);
        LocalDateTime cursor = paidAt;
        for (Interval closed : merged) {
            if (!closed.end().isAfter(cursor)) continue;
            Duration open = closed.start().isAfter(cursor) ? Duration.between(cursor, closed.start()) : Duration.ZERO;
            if (open.compareTo(remaining) >= 0) return cursor.plus(remaining);
            remaining = remaining.minus(open);
            cursor = closed.end();
        }
        return cursor.plus(remaining);
    }

    private static List<Interval> merged(Collection<Interval> closures) {
        List<Interval> merged = new ArrayList<>();
        closures.stream().filter(i -> i.start() != null && i.end() != null && i.end().isAfter(i.start()))
                .sorted(Comparator.comparing(Interval::start)).forEach(i -> {
                    if (merged.isEmpty() || i.start().isAfter(merged.get(merged.size()-1).end())) merged.add(i);
                    else {
                        Interval last = merged.remove(merged.size()-1);
                        merged.add(new Interval(last.start(), last.end().isAfter(i.end()) ? last.end() : i.end()));
                    }
                });
        return merged;
    }
    public static Duration openTimeBetween(LocalDateTime start,LocalDateTime end,Collection<Interval> closures) {
        Duration result=Duration.between(start,end);
        for(Interval interval:merged(closures)) {
            LocalDateTime a=interval.start().isAfter(start)?interval.start():start;
            LocalDateTime b=interval.end().isBefore(end)?interval.end():end;
            if(b.isAfter(a))result=result.minus(Duration.between(a,b));
        }
        return result;
    }

    public List<Interval> intervals(MemberPackage subscription) {
        List<Interval> result = new ArrayList<>();
        holidays.findAllByOrderByHolidayDateAsc().forEach(h -> {
            if(h.isActive())result.add(new Interval(h.getHolidayDate().atStartOfDay(), h.getHolidayDate().plusDays(1).atStartOfDay()));
            if(h.getActualClosedUntil()!=null)result.add(new Interval(h.getHolidayDate().atStartOfDay(),h.getActualClosedUntil()));
        });
        if (subscription.getSubscriptionId() != null) pauses.findBySubscriptionSubscriptionId(subscription.getSubscriptionId())
                .forEach(p -> result.add(new Interval(p.getStartsAt(), p.getEndsAt())));
        return result;
    }

    public LocalDateTime calculate(MemberPackage subscription) {
        return deadline(subscription.getActivatedAt(), Duration.ofSeconds(subscription.getApprovedExtraSeconds()).plusNanos(subscription.getApprovedExtraNanos()), intervals(subscription));
    }

    public void recalculate(MemberPackage subscription) {
        subscription.setExpiresAt(calculate(subscription));
        subscription.setEndDate(subscription.getExpiresAt().toLocalDate());
    }

    public static boolean validThrough(MemberPackage subscription, LocalDateTime end) {
        return subscription.getActivatedAt() != null && subscription.getExpiresAt() != null
                && !end.isAfter(subscription.getExpiresAt()) && !end.isBefore(subscription.getActivatedAt());
    }
}
