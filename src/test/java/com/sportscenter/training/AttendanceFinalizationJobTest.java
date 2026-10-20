package com.sportscenter.training;

import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceFinalizationJobTest {
    @Mock SessionRepository sessionRepository;
    @Mock AttendanceFinalizationProcessor processor;
    @Mock Clock clock;
    @InjectMocks AttendanceFinalizationJob job;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        ReflectionTestUtils.setField(job, "lookbackDays", 3);
    }

    @Test
    void finalizeEndedSessions_SkipsSessionsOlderThanConfiguredLookback() {
        Session recent = session(20, LocalDate.of(2026, 10, 15));
        Session tooOld = session(21, LocalDate.of(2026, 10, 12));
        when(sessionRepository.findSessionsReadyForAttendanceFinalization(
                LocalDate.of(2026, 10, 16), LocalTime.of(10, 0),
                LocalDate.of(2026, 10, 13), LocalTime.of(10, 0)))
                .thenReturn(List.of(recent, tooOld));

        job.finalizeEndedSessions();

        verify(processor).process(20);
        verify(processor, never()).process(21);
    }

    private Session session(Integer id, LocalDate date) {
        Session session = new Session();
        session.setId(id);
        session.setSessionDate(date);
        session.setEndTime(LocalTime.of(9, 0));
        return session;
    }
}
