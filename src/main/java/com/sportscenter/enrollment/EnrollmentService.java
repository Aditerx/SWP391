package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.invoice.InvoiceOrderResponse;
import com.sportscenter.invoice.InvoiceService;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private final ClassEnrollmentRepository enrollmentRepository;
    private final SportsClassRepository classRepository;
    private final UserRepository userRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuditService auditService;
    private final InvoiceService invoiceService;
    private final Clock clock;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAll(Integer classId, Integer memberId, String status, String actorEmail) {
        User actor = requireActor(actorEmail);
        if (isMemberActor(actor)) {
            memberId = resolveMemberScope(actor, memberId);
        }
        return enrollmentRepository.findFiltered(classId, memberId, status)
                .stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByClassId(Integer classId) {
        return enrollmentRepository.findByClassIdWithDetails(classId)
                .stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByMemberId(Integer memberId, String actorEmail) {
        memberId = resolveMemberScope(requireActor(actorEmail), memberId);
        return enrollmentRepository.findByMemberIdWithDetails(memberId).stream().map(enrollment -> {
            Integer position = "Waitlisted".equalsIgnoreCase(enrollment.getStatus())
                    ? waitlistPosition(enrollment) : null;
            return EnrollmentResponse.from(enrollment, position);
        }).toList();
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail) {
        return enroll(classId, targetMemberId, actorEmail, "Cash");
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail, String paymentMethod) {
        return enroll(classId, targetMemberId, actorEmail, paymentMethod, "127.0.0.1");
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail, String paymentMethod, String clientIp) {
        User actor = requireActor(actorEmail);
        final Integer memberId = resolveMemberScope(actor, targetMemberId);
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + memberId));

        // Validate class
        SportsClass sportsClass = classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        if (!"Active".equalsIgnoreCase(member.getStatus())) {
            throw new BusinessException("MEMBER_NOT_ACTIVE: member account is not active");
        }
        Member profile = memberRepository.findById(memberId).orElse(null);
        if (profile != null && profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("BOOKING_BLOCKED: booking is blocked until " + profile.getBookingBlockedUntil());
        }
        List<MemberPackage> activePackages = memberPackageRepository.findByMemberId(memberId).stream()
                .filter(p -> "Active".equalsIgnoreCase(p.getStatus()) && p.getEndDate() != null
                        && !p.getEndDate().isBefore(LocalDate.now(clock))).toList();
        if (activePackages.isEmpty()) throw new BusinessException("PACKAGE_EXPIRED: member package is expired or inactive");

        if (!"Open".equalsIgnoreCase(sportsClass.getStatus()) && !"Ongoing".equalsIgnoreCase(sportsClass.getStatus())) {
            throw new BusinessException("CLASS_NOT_OPEN: class status is '" + sportsClass.getStatus() + "'");
        }

        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(memberId, classId).orElse(null);
        if (enrollment != null && List.of("Registered", "Pending", "Waitlisted").contains(enrollment.getStatus())) {
            throw new BusinessException("ALREADY_ENROLLED: member is already enrolled in this class");
        }

        List<Session> conflicts = sessionRepository.findFutureEnrollmentScheduleConflicts(memberId, classId,
                LocalDate.now(clock), LocalTime.now(clock));
        if (!conflicts.isEmpty()) {
            Session conflict = conflicts.get(0);
            throw new BusinessException("SCHEDULE_CONFLICT: class " + conflict.getSportsClass().getId() +
                    " on " + conflict.getSessionDate() + " " + conflict.getStartTime() + "-" + conflict.getEndTime());
        }

        long queued = enrollmentRepository.countBySportsClassIdAndStatus(classId, "Waitlisted");
        long currentEnrolled = enrollmentRepository.countBySportsClassIdAndStatusIn(classId, List.of("Registered", "Pending"));
        boolean shouldWaitlist = (sportsClass.getMaxCapacity() != null && currentEnrolled >= sportsClass.getMaxCapacity()) || queued > 0;
        if (shouldWaitlist && queued >= 10) {
            throw new BusinessException("WAITLIST_FULL: class waitlist is full");
        }

        if (enrollment == null) {
            enrollment = new ClassEnrollment();
            enrollment.setMember(member);
            enrollment.setSportsClass(sportsClass);
        } else {
            enrollment.setStatus("Registered");
            enrollment.setCancelledAt(null);
            enrollment.setCancelledBy(null);
            enrollment.setCancelReason(null);
        }
        enrollment.setEnrolledAt(LocalDateTime.now(clock));
        if (shouldWaitlist) {
            enrollment.setStatus("Waitlisted");
            ClassEnrollment saved = enrollmentRepository.save(enrollment);
            auditService.log(actor.getId(), "ENROLL_CLASS", "CLASS_ENROLLMENT", saved.getId(),
                    "Member " + memberId + " added to waitlist for class " + classId);
            return EnrollmentResponse.from(saved, (int) queued + 1);
        }

        InvoiceOrderResponse order = null;
        enrollment.setStatus(statusForNewEnrollment(sportsClass));
        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        if ("Pending".equals(enrollment.getStatus())) {
            order = invoiceService.createClassOrder(member, sportsClass, paymentMethod == null ? "Cash" : paymentMethod, clientIp);
        }
        auditService.log(actor.getId(), "ENROLL_CLASS", "CLASS_ENROLLMENT", saved.getId(),
                "Member " + member.getFullName() + " (ID: " + memberId + ") enrolled in class '" + sportsClass.getName() + "' (ID: " + classId + ")");

        return order != null ? EnrollmentResponse.from(saved, order) : EnrollmentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(Integer classId, Integer targetMemberId, String actorEmail) {
        return cancelEnrollment(classId, targetMemberId, actorEmail, null);
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(Integer classId, Integer targetMemberId, String actorEmail, String reason) {
        User actor = requireActor(actorEmail);
        final Integer memberId = resolveMemberScope(actor, targetMemberId);
        classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(memberId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("No enrollment found for member " + memberId + " in class " + classId));

        if ("Cancelled".equalsIgnoreCase(enrollment.getStatus()) || "Completed".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException("ALREADY_CANCELLED: enrollment cannot be cancelled in status " + enrollment.getStatus());
        }
        checkCancellationPolicy(actor, enrollment, reason);

        if ("Pending".equalsIgnoreCase(enrollment.getStatus())) {
            invoiceService.cancelPendingClassPayment(memberId, classId);
        }

        String previousStatus = enrollment.getStatus();
        enrollment.setStatus("Cancelled");
        enrollment.setCancelledAt(LocalDateTime.now(clock));
        enrollment.setCancelledBy(actor);
        enrollment.setCancelReason(reason);
        ClassEnrollment saved = enrollmentRepository.save(enrollment);

        auditService.log(actor.getId(), "CANCEL_ENROLLMENT", "CLASS_ENROLLMENT", saved.getId(),
                "Member ID " + memberId + " cancelled enrollment in class ID " + classId);
        if (List.of("Registered", "Pending").contains(previousStatus)) promote(classId, actor.getId());

        return EnrollmentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse cancelById(Integer enrollmentId, String actorEmail) {
        return cancelById(enrollmentId, actorEmail, null);
    }

    @Transactional
    public EnrollmentResponse cancelById(Integer enrollmentId, String actorEmail, String reason) {
        User actor = requireActor(actorEmail);
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        resolveMemberScope(actor, enrollment.getMember() != null ? enrollment.getMember().getId() : null);
        Integer classId = enrollment.getSportsClass() != null ? enrollment.getSportsClass().getId() : null;
        if (classId == null) throw new ResourceNotFoundException("Class not found for enrollment: " + enrollmentId);
        classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        if ("Cancelled".equalsIgnoreCase(enrollment.getStatus()) || "Completed".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException("ALREADY_CANCELLED: enrollment cannot be cancelled in status " + enrollment.getStatus());
        }
        checkCancellationPolicy(actor, enrollment, reason);

        String previousStatus = enrollment.getStatus();
        if ("Pending".equalsIgnoreCase(enrollment.getStatus()) && enrollment.getMember() != null && enrollment.getSportsClass() != null) {
            invoiceService.cancelPendingClassPayment(enrollment.getMember().getId(), enrollment.getSportsClass().getId());
        }

        enrollment.setStatus("Cancelled");
        enrollment.setCancelledAt(LocalDateTime.now(clock));
        enrollment.setCancelledBy(actor);
        enrollment.setCancelReason(reason);
        ClassEnrollment saved = enrollmentRepository.save(enrollment);

        auditService.log(actor.getId(), "CANCEL_ENROLLMENT", "CLASS_ENROLLMENT", saved.getId(),
                "Cancelled enrollment ID " + enrollmentId);
        if (List.of("Registered", "Pending").contains(previousStatus)) promote(classId, actor.getId());

        return EnrollmentResponse.from(saved);
    }

    private void checkCancellationPolicy(User actor, ClassEnrollment enrollment, String reason) {
        if (!isMemberActor(actor)) {
            if (reason == null || reason.isBlank()) throw new BusinessException("Cancellation reason is required");
            return;
        }
        if ("Waitlisted".equalsIgnoreCase(enrollment.getStatus())) return;
        Integer classId = enrollment.getSportsClass().getId();
        LocalDateTime now = LocalDateTime.now(clock);
        List<Session> upcoming = sessionRepository.findNextUpcomingForClass(classId, now.toLocalDate(), now.toLocalTime());
        if (!upcoming.isEmpty()) {
            Session next = upcoming.get(0);
            LocalDateTime startsAt = LocalDateTime.of(next.getSessionDate(), next.getStartTime());
            if (java.time.Duration.between(now, startsAt).compareTo(java.time.Duration.ofHours(2)) < 0) {
                throw new BusinessException("CANCEL_TOO_LATE: class starts in less than two hours");
            }
        }
    }

    private Integer waitlistPosition(ClassEnrollment enrollment) {
        return (int) enrollmentRepository.countBySportsClassIdAndStatusAndEnrolledAtLessThan(
                enrollment.getSportsClass().getId(), "Waitlisted", enrollment.getEnrolledAt()) + 1;
    }

    @Transactional
    public void promoteWaitlist(Integer classId, Integer actorId) {
        promote(classId, actorId);
    }

    private void promote(Integer classId, Integer actorId) {
        SportsClass sportsClass = classRepository.findById(classId).orElse(null);
        if (sportsClass == null || sportsClass.getMaxCapacity() == null) return;
        long occupied = enrollmentRepository.countBySportsClassIdAndStatusIn(classId, List.of("Registered", "Pending"));
        int free = Math.max(0, sportsClass.getMaxCapacity() - (int) occupied);
        for (ClassEnrollment waiting : enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(classId, "Waitlisted")) {
            if (free <= 0) break;
            User member = waiting.getMember();
            if (member == null || !"Active".equalsIgnoreCase(member.getStatus())) continue;
            boolean packageActive = memberPackageRepository.findByMemberId(member.getId()).stream()
                    .anyMatch(p -> "Active".equalsIgnoreCase(p.getStatus()) && p.getEndDate() != null && !p.getEndDate().isBefore(LocalDate.now(clock)));
            if (!packageActive) continue;
            Member profile = memberRepository.findById(member.getId()).orElse(null);
            if (profile != null && profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) continue;
            if (!sessionRepository.findFutureEnrollmentScheduleConflicts(member.getId(), classId,
                    LocalDate.now(clock), LocalTime.now(clock)).isEmpty()) continue;
            waiting.setStatus(statusForNewEnrollment(sportsClass));
            waiting.setPromotedAt(LocalDateTime.now(clock));
            ClassEnrollment promoted = enrollmentRepository.save(waiting);
            auditService.log(actorId, "PROMOTE_WAITLIST", "CLASS_ENROLLMENT", promoted.getId(),
                    "Promoted member " + member.getId() + " from waitlist for class " + classId);
            safeNotify(member.getId(), "Waitlist update", "A place opened in " + sportsClass.getName() + ". Your status is " + waiting.getStatus() + ".", "Schedule");
            free--;
        }
    }

    private void safeNotify(Integer userId, String title, String content, String type) {
        try {
            notificationService.send(userId, title, content, type);
        } catch (RuntimeException error) {
            log.warn("Unable to send notification to user {}: {}", userId, error.getMessage());
        }
    }

    private String statusForNewEnrollment(SportsClass sportsClass) {
        return sportsClass.getTuitionFee() != null && sportsClass.getTuitionFee().signum() > 0
                ? "Pending" : "Registered";
    }

    private User requireActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
    }

    private Integer resolveMemberScope(User actor, Integer requestedMemberId) {
        if (isMemberActor(actor)) {
            if (requestedMemberId != null && !requestedMemberId.equals(actor.getId())) {
                throw new AccessDeniedException("Members may only access their own enrollments");
            }
            return actor.getId();
        }
        if (requestedMemberId == null) {
            throw new BusinessException("Member ID must be specified");
        }
        return requestedMemberId;
    }

    private boolean isMemberActor(User actor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean memberAuthority = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()));
        return memberAuthority || (actor.getRole() != null && "Member".equalsIgnoreCase(actor.getRole().getName()));
    }
}
