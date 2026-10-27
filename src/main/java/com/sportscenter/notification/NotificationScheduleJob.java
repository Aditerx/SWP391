package com.sportscenter.notification;

import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationScheduleJob {
    private final SessionRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 900_000L)
    @Transactional(readOnly = true)
    public void sendSessionReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = now.plusHours(24);
        LocalDateTime until = from.plusMinutes(15);
        for (Session session : sessionRepository.findScheduledBetweenDates(from.toLocalDate(), until.toLocalDate())) {
            if (!"Scheduled".equalsIgnoreCase(session.getStatus())) continue;
            LocalDateTime startsAt = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
            if (startsAt.isBefore(from) || startsAt.isAfter(until) || !startsAt.isAfter(now)) continue;
            Set<Integer> recipients = sessionRecipients(session);
            String message = NotificationTemplates.sessionReminder(subjectName(session), startsAt);
            for (Integer userId : recipients) {
                eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.SESSION_REMINDER,
                        List.of(userId), message, "SESSION", session.getId(),
                        "SESSION_REMINDER:" + session.getId() + ":" + userId + ":24h"));
            }
        }
    }

    @Scheduled(cron = "${scms.notification.package-expiring-cron:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional(readOnly = true)
    public void notifyPackagesExpiringSoon() {
        LocalDate today = LocalDate.now(clock);
        for (MemberPackage memberPackage : memberPackageRepository.findActiveAdvancedExpiringBetween(today, today.plusDays(3))) {
            User member = memberPackage.getMember();
            eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.PACKAGE_EXPIRING_SOON,
                    List.of(member.getId()),
                    String.format("Gói %s của bạn sẽ hết hạn ngày %s.",
                            memberPackage.getMembershipPackage().getName(), memberPackage.getEndDate()),
                    "MEMBER_PACKAGE", memberPackage.getSubscriptionId(),
                    "PACKAGE_EXPIRING_SOON:" + memberPackage.getSubscriptionId() + ":" + member.getId()
                            + ":" + memberPackage.getEndDate()));
        }
    }

    private Set<Integer> sessionRecipients(Session session) {
        Set<Integer> recipients = new LinkedHashSet<>();
        User coach = session.getCoach() != null ? session.getCoach()
                : session.getSportsClass() == null ? null : session.getSportsClass().getCoach();
        if (coach != null) recipients.add(coach.getId());
        if (session.getBookedMember() != null) {
            recipients.add(session.getBookedMember().getId());
        } else if (session.getSportsClass() != null) {
            enrollmentRepository.findByClassIdWithDetails(session.getSportsClass().getId()).stream()
                    .filter(enrollment -> "Registered".equalsIgnoreCase(enrollment.getStatus()))
                    .map(enrollment -> enrollment.getMember().getId()).forEach(recipients::add);
        }
        return recipients;
    }

    private String subjectName(Session session) {
        return session.getSportsClass() == null || session.getSportsClass().getSubject() == null
                ? "lớp học" : session.getSportsClass().getSubject().getName();
    }
}
