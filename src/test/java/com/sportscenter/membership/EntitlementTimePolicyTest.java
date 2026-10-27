package com.sportscenter.membership;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EntitlementTimePolicyTest {
    LocalDateTime paid = LocalDateTime.of(2026,1,1,15,30);
    EntitlementTimePolicy.Interval closed(LocalDateTime a,LocalDateTime b) { return new EntitlementTimePolicy.Interval(a,b); }
    @Test void paidTimestampAndExactDeadlineArePreserved() {
        assertEquals(paid.plusDays(45),EntitlementTimePolicy.deadline(paid,0,List.of()));
    }
    @Test void closuresAtPaymentAndInExtendedTailAreIncludedOnce() {
        var closures=List.of(closed(paid.minusHours(2),paid.plusHours(3)),
                closed(paid.plusDays(45),paid.plusDays(46)),closed(paid.plusDays(45),paid.plusDays(46)));
        assertEquals(paid.plusDays(46).plusHours(3),EntitlementTimePolicy.deadline(paid,0,closures));
    }
    @Test void overlappingIncidentAndHolidayNeverDoubleCompensate() {
        var holiday=closed(paid.plusDays(2),paid.plusDays(4));
        var incident=closed(paid.plusDays(3),paid.plusDays(5));
        assertEquals(paid.plusDays(48),EntitlementTimePolicy.deadline(paid,0,List.of(holiday,incident)));
    }
    @Test void closureStartingExactlyAtDeadlineDoesNotAddTime() {
        assertEquals(paid.plusDays(45),EntitlementTimePolicy.deadline(paid,0,List.of(closed(paid.plusDays(45),paid.plusDays(46)))));
    }
    @Test void fractionalPaymentTimestampSurvivesMidnightClosure() {
        var precise=paid.plusNanos(123456000);
        var midnight=paid.toLocalDate().plusDays(1).atStartOfDay();
        assertEquals(precise.plusDays(46),EntitlementTimePolicy.deadline(precise,0,List.of(closed(midnight,midnight.plusDays(1)))));
    }
}
