package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.notification.NotificationTemplates;
import com.sportscenter.notification.NotificationType;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceFinalizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(AttendanceFinalizationProcessor.class);
    private final SessionRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final com.sportscenter.membership.MemberPackageRepository memberPackageRepository;
    @org.springframework.beans.factory.annotation.Value("${scms.booking-block.enabled:false}")
    private boolean bookingBlockEnabled;
    private final ApplicationEventPublisher eventPublisher;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Integer sessionId) {
        var center=sessionRepository.findCenterIdById(sessionId);
        if(center.isEmpty())return;
        scheduleWriteLock.acquire(center.get());
        Session session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null || session.isAttendanceFinalized() || session.isCenterInvalidated() || "Cancelled".equalsIgnoreCase(session.getStatus())) return;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime endsAt = LocalDateTime.of(session.getSessionDate(), session.getEndTime());
        if (endsAt.isAfter(now)) return;
        List<Integer> noShowMemberIds = new java.util.ArrayList<>();
        if (session.getBookedMember() != null) {
            Integer memberId = session.getBookedMember().getId();
            if (attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId).isEmpty()) {
                Attendance absent = new Attendance();
                absent.setSession(session);
                absent.setMember(session.getBookedMember());
                absent.setState("Absent");
                absent.setRecordedBy(null);
                attendanceRepository.save(absent);
                noShowMemberIds.add(memberId);
                auditService.log(null, "NO_SHOW_PRIVATE_SESSION", "SESSION", sessionId,
                        "Member " + memberId + " marked Absent (No-show) for private session " + sessionId + ". 1 credit consumed.");
            }
        } else {
            List<ClassEnrollment> registered = enrollmentRepository.findByClassIdWithDetails(session.getSportsClass().getId()).stream()
                    .filter(e -> "Registered".equalsIgnoreCase(e.getStatus())).toList();
            for (ClassEnrollment enrollment : registered) {
                Integer memberId = enrollment.getMember().getId();
                if (attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId).isPresent()) continue;
                Attendance absent = new Attendance();
                absent.setSession(session);
                absent.setMember(enrollment.getMember());
                absent.setState("Absent");
                absent.setRecordedBy(null);
                attendanceRepository.save(absent);
                noShowMemberIds.add(memberId);

                LocalDate monthStart = now.toLocalDate().withDayOfMonth(1);
                long absences = attendanceRepository.countAbsencesForMemberInRange(memberId, monthStart, monthStart.plusMonths(1));
                Member member = memberRepository.findByIdForUpdate(memberId).orElse(null);
                if (bookingBlockEnabled && absences >= 3 && member != null && (member.getBookingBlockedUntil() == null || !member.getBookingBlockedUntil().isAfter(now))) {
                    member.setBookingBlockedUntil(now.plusDays(7));
                    memberRepository.save(member);
                    auditService.log(null, "BOOKING_BLOCK_APPLIED", "members", memberId,
                            "Blocked class bookings until " + member.getBookingBlockedUntil() + " after " + absences + " absences");
                    safeNotify(memberId, "Class booking restricted", "Class bookings are blocked for seven days after repeated absences.", "System");
                }
            }
        }
        session.setAttendanceFinalized(true);
        session.setStatus("Completed");
        sessionRepository.save(session);
        if (session.getBookedMember() != null && session.getMemberPackage() != null) {
            var sub = memberPackageRepository.findByIdForUpdate(session.getMemberPackage().getSubscriptionId()).orElse(null);
            if (sub != null && "Active".equals(sub.getStatus()) && Integer.valueOf(0).equals(sub.getRemainingCredit())
                    && sessionRepository.countByMemberPackageSubscriptionIdAndStatusAndAttendanceFinalizedFalse(sub.getSubscriptionId(), "Scheduled") == 0) {
                sub.setStatus("Completed"); memberPackageRepository.save(sub);
            }
        }
        if (session.getBookedMember() == null && session.getSportsClass().isNormal()
                && sessionRepository.countBySportsClassIdAndCenterInvalidatedFalseAndStatusNot(session.getSportsClass().getId(), "Cancelled") == 12
                && sessionRepository.countBySportsClassIdAndStatusAndAttendanceFinalizedFalse(session.getSportsClass().getId(), "Scheduled") == 0) {
            enrollmentRepository.findByClassIdWithDetails(session.getSportsClass().getId()).stream()
                    .filter(e -> "Registered".equals(e.getStatus())).forEach(e -> {
                        e.setStatus("Completed"); enrollmentRepository.save(e);
                    });
        }
        if (!noShowMemberIds.isEmpty()) {
            eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.NO_SHOW_RECORDED,
                    noShowMemberIds,
                    NotificationTemplates.noShow(session.getSessionDate(), session.getStartTime(), session.getEndTime()),
                    "SESSION", session.getId(), "NO_SHOW_RECORDED:" + session.getId()));
        }
    }

    private void safeNotify(Integer userId, String title, String content, String type) {
        try { notificationService.send(userId, title, content, type); }
        catch (RuntimeException error) { log.warn("Unable to send notification to user {}: {}", userId, error.getMessage()); }
    }
}
