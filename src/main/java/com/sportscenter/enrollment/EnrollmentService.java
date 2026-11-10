package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.AccountScope;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
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
    private final AuditService auditService;
    private final InvoiceService invoiceService;
    private final Clock clock;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAll(Integer classId, Integer memberId, String status, String actorEmail) {
        User actor = requireReadActor(actorEmail);
        if (classId != null) requireClassRead(actor, classId);
        if (isMemberActor(actor)) {
            memberId = resolveMemberScope(actor, memberId);
        }
        return enrollmentRepository.findFiltered(classId, memberId, status)
                .stream().filter(e -> readable(actor, e)).map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByClassId(Integer classId, String actorEmail) {
        User actor = requireReadActor(actorEmail);
        requireClassRead(actor, classId);
        return enrollmentRepository.findByClassIdWithDetails(classId)
                .stream().filter(e -> readable(actor, e)).map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByMemberId(Integer memberId, String actorEmail) {
        User actor = requireReadActor(actorEmail);
        memberId = resolveMemberScope(actor, memberId);
        User member = userRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        AccountScope.require(actor, member.getCenterId());
        return enrollmentRepository.findByMemberIdWithDetails(memberId).stream().filter(e -> readable(actor, e)).map(enrollment -> {
            Integer position = "Waitlisted".equalsIgnoreCase(enrollment.getStatus())
                    ? waitlistPosition(enrollment) : null;
            return EnrollmentResponse.from(enrollment, position);
        }).toList();
    }

    private User requireReadActor(String email) {
        User actor = requireActor(email);
        if (!"Active".equalsIgnoreCase(actor.getStatus()) || actor.getRole() == null)
            throw new AccessDeniedException("Active account required");
        return actor;
    }

    private void requireClassRead(User actor, Integer classId) {
        SportsClass c = classRepository.findById(classId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        AccountScope.require(actor, c.getCenterId());
        if (ActorPolicy.role(actor, "Coach") && (c.getCoach() == null || !actor.getId().equals(c.getCoach().getId())))
            throw new AccessDeniedException("Coach is not assigned to this class");
    }

    private boolean readable(User actor, ClassEnrollment e) {
        SportsClass c = e.getSportsClass();
        return c != null && e.getMember() != null && AccountScope.visible(actor, c.getCenterId())
                && AccountScope.visible(actor, e.getMember().getCenterId())
                && (!ActorPolicy.role(actor, "Coach") || c.getCoach() != null && actor.getId().equals(c.getCoach().getId()));
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail) {
        return enroll(classId, targetMemberId, actorEmail, "VNPay");
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail, String paymentMethod) {
        return enroll(classId, targetMemberId, actorEmail, paymentMethod, "127.0.0.1");
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail, String paymentMethod, String clientIp) {
        User actor = requireActor(actorEmail);
        if (!"Active".equalsIgnoreCase(actor.getStatus())) throw new AccessDeniedException("Active account required");
        final Integer memberId = resolveMemberScope(actor, targetMemberId);
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + memberId));
        scheduleWriteLock.acquire(member.getCenterId());

        // Validate class
        SportsClass sportsClass = classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        if (!java.util.Objects.equals(member.getCenterId(), sportsClass.getCenterId())) throw new AccessDeniedException("Class belongs to another center");
        if (!sportsClass.isNormal()) {
            throw new BusinessException("USE_ADVANCED_REGISTRATION: Advanced requires subject selection and Coach acceptance before payment");
        }
        if (sportsClass.getSubject() != null && !"Active".equalsIgnoreCase(sportsClass.getSubject().getStatus())) {
            throw new BusinessException("SUBJECT_INACTIVE: New registrations are closed for this subject");
        }

        if (!"Active".equalsIgnoreCase(member.getStatus())) {
            throw new BusinessException("MEMBER_NOT_ACTIVE: member account is not active");
        }
        Member profile = memberRepository.findById(memberId).orElse(null);
        if (profile != null && profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("BOOKING_BLOCKED: booking is blocked until " + profile.getBookingBlockedUntil());
        }
        if (!"Open".equalsIgnoreCase(sportsClass.getStatus()) && !"Ongoing".equalsIgnoreCase(sportsClass.getStatus())) {
            throw new BusinessException("CLASS_NOT_OPEN: class status is '" + sportsClass.getStatus() + "'");
        }
        if (sportsClass.isNormal()) {
            if (!"Open".equalsIgnoreCase(sportsClass.getStatus())) throw new BusinessException("CLASS_NOT_READY: Normal registration is closed");
            requirePositiveNormalPrice(sportsClass);
            com.sportscenter.session.NormalSchedulePolicy.requireReady(sportsClass, sessionRepository, clock);
            com.sportscenter.sportclass.NormalClassPolicy.requireAdmission(sportsClass, sessionRepository, clock);
        }

        String method = paymentMethod == null ? "VNPay" : paymentMethod;
        boolean cash = "Cash".equalsIgnoreCase(method);
        if (cash && (!ActorPolicy.role(actor, "Receptionist") || !hasAuthority("MANAGE_INVOICES") || !hasAuthority("REGISTER_MEMBER")))
            throw new AccessDeniedException("NORMAL_CASH_COUNTER_ONLY: Normal Cash requires Receptionist counter registration and immediate collection");
        if (!cash && !"VNPay".equalsIgnoreCase(method)) throw new BusinessException("Payment method must be Cash or VNPay");
        invoiceService.expireClassReservations(sportsClass);
        rotateExpiredInvitations(sportsClass);

        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(memberId, classId).orElse(null);
        boolean claimingWaitlist = enrollment != null && "Waitlisted".equals(enrollment.getStatus());
        if (enrollment != null && List.of("Registered", "Pending").contains(enrollment.getStatus())) {
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
        boolean queueTurn = claimingWaitlist && enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(classId, "Waitlisted")
                .stream().findFirst().map(e -> e.getMember().getId().equals(memberId)).orElse(false);
        boolean shouldWaitlist = (sportsClass.getMaxCapacity() != null && currentEnrolled >= sportsClass.getMaxCapacity()) || queued > 0 && !queueTurn;
        if (claimingWaitlist && shouldWaitlist) throw new BusinessException("WAITLIST_NOT_READY: No available place for this queue entry");
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
            if (cash) throw new BusinessException("NORMAL_CASH_NO_SEAT: Cash cannot be collected for a waitlist place");
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
            order = invoiceService.createClassOrder(member, sportsClass, method, clientIp);
            if (cash) {
                invoiceService.confirmCash(order.invoiceId(), SecurityContextHolder.getContext().getAuthentication());
                order = new InvoiceOrderResponse(order.invoiceId(), order.invoiceCode(), order.amount(), order.paymentMethod(), "Paid", order.expiresAt(), null);
            }
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
        scheduleWriteLock.acquire(actor.getCenterId());
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
        scheduleWriteLock.acquire(actor.getCenterId());
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
        if (enrollment.getSportsClass() != null && enrollment.getSportsClass().isNormal() && !"Waitlisted".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException("NORMAL_CLASS_NON_CANCELLABLE: Normal registrations cannot be cancelled through this endpoint");
        }
        if (!isMemberActor(actor)) {
            if (reason == null || reason.isBlank()) throw new BusinessException("Cancellation reason is required");
            return;
        }
        if ("Waitlisted".equalsIgnoreCase(enrollment.getStatus())) return;

        // Ràng buộc Khóa Normal: CẤM HỦY / BÁO VẮNG (Member.pdf FR-015, RP-17, AGENTS.md 2.4)
        if (enrollment.getSportsClass() != null && enrollment.getSportsClass().isNormal()) {
            throw new BusinessException("NORMAL_CLASS_NON_CANCELLABLE: Lớp học nhóm cố định 12 buổi không hỗ trợ hủy hoặc hoàn tiền theo quy chế trung tâm.");
        }

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
        scheduleWriteLock.acquire(classRepository.findCenterIdById(classId).orElseThrow(() -> new ResourceNotFoundException("Class not found")));
        promote(classId, actorId);
    }

    private void promote(Integer classId, Integer actorId) {
        SportsClass sportsClass = classRepository.findByIdForUpdate(classId).orElse(null);
        if (sportsClass == null || !sportsClass.isNormal() || sportsClass.getMaxCapacity() == null) return;
        // A notification invites a fresh authenticated order; promotion never manufactures a Cash invoice.
        try { com.sportscenter.sportclass.NormalClassPolicy.requireAdmission(sportsClass, sessionRepository, clock); }
        catch (BusinessException closed) { return; }
        invoiceService.expireClassReservations(sportsClass);
        rotateExpiredInvitations(sportsClass);
        requirePositiveNormalPrice(sportsClass);
        long occupied = enrollmentRepository.countBySportsClassIdAndStatusIn(classId, List.of("Registered", "Pending"));
        int free = Math.max(0, sportsClass.getMaxCapacity() - (int) occupied);
        for (ClassEnrollment waiting : enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(classId, "Waitlisted")) {
            if (free <= 0) break;
            if (waiting.getPromotedAt() != null) { free--; continue; }
            User member = waiting.getMember();
            if (member == null || !"Active".equalsIgnoreCase(member.getStatus())) continue;
            Member profile = memberRepository.findById(member.getId()).orElse(null);
            if (profile != null && profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) continue;
            if (!sessionRepository.findFutureEnrollmentScheduleConflicts(member.getId(), classId,
                    LocalDate.now(clock), LocalTime.now(clock)).isEmpty()) continue;
            waiting.setPromotedAt(LocalDateTime.now(clock));
            ClassEnrollment promoted = enrollmentRepository.save(waiting);
            auditService.log(actorId, "PROMOTE_WAITLIST", "CLASS_ENROLLMENT", promoted.getId(),
                    "Promoted member " + member.getId() + " from waitlist for class " + classId);
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { safeNotify(member.getId(), "Waitlist update", "A place opened in " + sportsClass.getName() + ". Create a VNPay order or register at the counter before the cutoff.", "Schedule"); }
            });
            free--;
        }
    }

    private boolean hasAuthority(String name) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> name.equals(a.getAuthority()));
    }

    @org.springframework.beans.factory.annotation.Value("${payment.expire-minutes:5}")
    private int invitationMinutes=5;

    private void rotateExpiredInvitations(SportsClass c) {
        LocalDateTime now=LocalDateTime.now(clock);
        for(ClassEnrollment e:enrollmentRepository.findByClassIdWithDetails(c.getId())) {
            if(e.getPromotedAt()==null)continue;
            boolean expiredInvite="Waitlisted".equals(e.getStatus())&&!e.getPromotedAt().plusMinutes(invitationMinutes).isAfter(now);
            boolean expiredOrder="Cancelled".equals(e.getStatus())&&invoiceService.hasExpiredClassOrder(e.getMember().getId(),c.getId());
            if(expiredInvite||expiredOrder) {
                e.setStatus("Waitlisted");e.setPromotedAt(null);e.setEnrolledAt(now);e.setCancelledAt(null);e.setCancelledBy(null);e.setCancelReason(null);
            }
        }
        enrollmentRepository.flush();
    }

    private void safeNotify(Integer userId, String title, String content, String type) {
        try {
            notificationService.send(userId, title, content, type);
        } catch (RuntimeException error) {
            log.warn("Unable to send notification to user {}: {}", userId, error.getMessage());
        }
    }

    private String statusForNewEnrollment(SportsClass sportsClass) {
        requirePositiveNormalPrice(sportsClass);
        return "Pending";
    }

    private void requirePositiveNormalPrice(SportsClass sportsClass) {
        if (sportsClass.getTuitionFee() == null || sportsClass.getTuitionFee().signum() <= 0) {
            throw new BusinessException("INVALID_PRICE: Normal class tuition must be positive");
        }
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
