package com.sportscenter.training;

import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AttendanceFinalizationJob {
    private static final Logger log = LoggerFactory.getLogger(AttendanceFinalizationJob.class);
    private final SessionRepository sessionRepository;
    private final AttendanceFinalizationProcessor processor;
    private final Clock clock;
    @Value("${scms.attendance.lookback-days:3}")
    private int lookbackDays;

    @Scheduled(fixedDelay = 600_000L)
    public void finalizeEndedSessions() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(lookbackDays);
        for (Session session : sessionRepository.findSessionsReadyForAttendanceFinalization(
                now.toLocalDate(), now.toLocalTime(), cutoff.toLocalDate(), cutoff.toLocalTime())) {
            LocalDateTime sessionEnd = LocalDateTime.of(session.getSessionDate(), session.getEndTime());
            if (sessionEnd.isBefore(cutoff)) continue;
            try { processor.process(session.getId()); }
            catch (RuntimeException error) { log.error("Attendance finalization failed for session {}", session.getId(), error); }
        }
    }
}
