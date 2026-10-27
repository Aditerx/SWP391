package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final java.util.concurrent.ConcurrentMap<Integer, ScanWindow> SCAN_ATTEMPTS = new java.util.concurrent.ConcurrentHashMap<>();
    private record ScanWindow(long startedAt, int count) { }
    private final AttendanceRepository attendanceRepository;
    private final TrainingResultRepository trainingResultRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @org.springframework.beans.factory.annotation.Value("${scms.lobby-checkin.enabled:false}") private boolean lobbyCheckinEnabled;

    @Transactional(readOnly = true)
    public List<AttendanceResponse> searchAttendances(
            Integer sessionId,
            Integer memberId,
            Integer recordedBy,
            String state,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        Integer resolvedMemberId = memberId;
        boolean memberRole = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()));
        if (memberRole) {
            User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated member account was not found"));
            if (resolvedMemberId != null && !resolvedMemberId.equals(currentUser.getId())) {
                throw new AccessDeniedException("Members may only view their own attendance records");
            }
            resolvedMemberId = currentUser.getId();
        }
        final Integer effectiveMemberId = resolvedMemberId;
        User actor = userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Account not found"));
        boolean coachRole = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_COACH".equals(a.getAuthority()));
        List<Attendance> all = attendanceRepository.findAllWithDetails();
        return all.stream()
                .filter(a -> a.getMember() != null && java.util.Objects.equals(actor.getCenterId(), a.getMember().getCenterId()))
                .filter(a -> !coachRole || a.getSession() != null && actor.getId().equals(assignedCoachId(a.getSession())))
                .filter(a -> sessionId == null || (a.getSession() != null && sessionId.equals(a.getSession().getId())))
                .filter(a -> effectiveMemberId == null || (a.getMember() != null && effectiveMemberId.equals(a.getMember().getId())))
                .filter(a -> recordedBy == null || (a.getRecordedBy() != null && recordedBy.equals(a.getRecordedBy().getId())))
                .filter(a -> state == null || state.isBlank() || (a.getState() != null && a.getState().equalsIgnoreCase(state)))
                .filter(a -> startDate == null || (a.getCheckInTime() != null && !a.getCheckInTime().isBefore(startDate)))
                .filter(a -> endDate == null || (a.getCheckInTime() != null && !a.getCheckInTime().isAfter(endDate)))
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional
    public AttendanceResponse checkIn(Integer memberId, String actorEmail) {
        if(!lobbyCheckinEnabled)throw new BusinessException("LOBBY_CHECKIN_DISABLED: Lobby check-in is reserved for a later release");
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        User recordedBy = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Attendance att = new Attendance();
        att.setSession(null);
        att.setMember(member);
        att.setRecordedBy(recordedBy);
        att.setState("CheckedIn");
        att.setCheckInTime(LocalDateTime.now(clock));

        Attendance saved = attendanceRepository.save(att);

        auditService.log(recordedBy != null ? recordedBy.getId() : null,
                "RECORD_RESULT", "attendances", saved.getAttendanceId(),
                "Member " + member.getFullName() + " checked in at counter");

        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(Integer memberId, String actorEmail) {
        if(!lobbyCheckinEnabled)throw new BusinessException("LOBBY_CHECKIN_DISABLED: Lobby check-out is reserved for a later release");
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        List<Attendance> openCheckIns = attendanceRepository.findOpenCheckInsByMemberId(memberId);
        if (openCheckIns.isEmpty()) {
            throw new BusinessException("No open check-in found for member #" + memberId);
        }

        Attendance att = openCheckIns.get(0);
        att.setCheckOutTime(LocalDateTime.now(clock));
        Attendance saved = attendanceRepository.save(att);

        auditService.log(actor.getId(), "RECORD_RESULT", "attendances", saved.getAttendanceId(),
                "Member #" + memberId + " checked out");

        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse correctAttendance(AttendanceCorrectionRequest request, String actorEmail) {
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        com.sportscenter.common.ActorPolicy.manager(actor);
        if(!"Active".equals(actor.getStatus()))throw new AccessDeniedException("Active Manager required");
        scheduleWriteLock.acquire(actor.getCenterId());
        Attendance att = attendanceRepository.findById(request.attendanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + request.attendanceId()));
        if (att.getMember() != null && !java.util.Objects.equals(actor.getCenterId(), att.getMember().getCenterId())) throw new AccessDeniedException("Attendance belongs to another center");
        if(att.getSession()==null)throw new BusinessException("SESSION_REQUIRED: This correction flow applies to session attendance");
        Session locked=sessionRepository.findByIdForUpdate(att.getSession().getId()).orElseThrow(()->new ResourceNotFoundException("Session not found"));
        if(LocalDateTime.now(clock).isBefore(locked.getSessionDate().atTime(locked.getEndTime())))throw new BusinessException("USE_SESSION_ATTENDANCE: Record in-session attendance before session end");
        if(locked.isCenterInvalidated())throw new BusinessException("SESSION_NOT_HELD: Preserve incident and attendance history");

        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException("A reason is required for attendance correction");
        }
        if (actor.getRole() != null && ("Coach".equalsIgnoreCase(actor.getRole().getName()) || "ROLE_COACH".equalsIgnoreCase(actor.getRole().getName()))) {
            if (att.getSession() == null || !actor.getId().equals(assignedCoachId(att.getSession()))) {
                throw new AccessDeniedException("Coaches may only correct attendance for their own classes");
            }
        }

        String oldState = att.getState();
        String newState = normalizeState(request.newState());
        att.setState(newState);

        Attendance saved = attendanceRepository.save(att);

        // Also sync training result if tied to a session
        if (att.getSession() != null && att.getMember() != null) {
            trainingResultRepository.findBySessionIdAndMemberId(att.getSession().getId(), att.getMember().getId())
                    .ifPresent(tr -> {
                        tr.setAttendanceStatus(newState);
                        trainingResultRepository.save(tr);
                    });
        }

        String auditReason = request.reason() != null && !request.reason().isBlank()
                ? request.reason()
                : "Manual attendance correction";

        auditService.log(actor.getId(), "ATTENDANCE_CORRECTION", "attendances", saved.getAttendanceId(),
                "Status: " + oldState + " -> " + newState + " | Reason: " + auditReason);

        return AttendanceResponse.from(saved);
    }

    /** Compatibility entry point: absence is now a separate session notice, never attendance. */
    @Deprecated
    public AttendanceResponse requestAbsence(Integer sessionId,String reason,String actorEmail) {
        throw new BusinessException("USE_ABSENCE_NOTICE: Report absence through /api/sessions/{id}/absence");
    }

    @Transactional
    public CheckinCodeResponse createCheckinCode(String actorEmail) {
        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated member not found"));
        if(!"Active".equalsIgnoreCase(user.getStatus()))throw new BusinessException("MEMBER_NOT_ACTIVE: Active Member required");
        Member member = memberRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        if (member.getCheckinSecret() == null) {
            member.setCheckinSecret(Totp.encodeSecret(Totp.randomSecret()));
            memberRepository.save(member);
        }
        long epoch = clock.instant().getEpochSecond();
        String payload = Totp.code(java.util.Base64.getDecoder().decode(member.getCheckinSecret()), epoch / 30);
        return new CheckinCodeResponse(payload, (int) (30 - epoch % 30));
    }

    @Transactional
    public AttendanceResponse scanCheckin(Integer memberId, String code, Integer sessionId, String actorEmail) {
        User recorder = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if(!"Active".equalsIgnoreCase(recorder.getStatus())||recorder.getRole()==null||!java.util.Set.of("Coach","Receptionist","CenterManager","Admin").contains(recorder.getRole().getName()))throw new AccessDeniedException("Active session staff required");
        if(sessionId==null)throw new BusinessException("SESSION_REQUIRED: Lobby QR check-in is not enabled");
        scheduleWriteLock.acquire(recorder.getCenterId());
        Session lockedSession=sessionRepository.findByIdForUpdate(sessionId).orElseThrow(()->new ResourceNotFoundException("Session not found"));
        com.sportscenter.common.ActorPolicy.center(recorder,lockedSession.getSportsClass().getCenterId());
        SessionAttendancePolicy.requireWindow(lockedSession,LocalDateTime.now(clock));
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
        Integer memberCenterId = member.getUser() != null ? member.getUser().getCenterId() : null;
        if (recorder.getCenterId() != null && memberCenterId != null
                && !recorder.getCenterId().equals(memberCenterId)) {
            throw new AccessDeniedException("Staff may only scan members in their center");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        long epoch = clock.instant().getEpochSecond();
        ScanWindow attemptWindow = SCAN_ATTEMPTS.get(memberId);
        if (attemptWindow != null && epoch - attemptWindow.startedAt() < 60 && attemptWindow.count() >= 5) {
            throw new BusinessException("INVALID_CHECKIN_CODE: scan limit reached");
        }
        if (attemptWindow != null && epoch - attemptWindow.startedAt() >= 60) SCAN_ATTEMPTS.remove(memberId, attemptWindow);
        Long step = member.getCheckinSecret() == null ? null : Totp.matchingStep(
                java.util.Base64.getDecoder().decode(member.getCheckinSecret()), code, epoch / 30);
        if (step == null || step != epoch / 30 || step.equals(member.getCheckinLastStep())) {
            SCAN_ATTEMPTS.compute(memberId, (id, current) -> current == null || epoch - current.startedAt() >= 60
                    ? new ScanWindow(epoch, 1) : new ScanWindow(current.startedAt(), current.count() + 1));
            throw new BusinessException("INVALID_CHECKIN_CODE: code is invalid, expired, or already used");
        }
        if (!"Active".equalsIgnoreCase(member.getUser().getStatus())) {
            throw new BusinessException("MEMBER_NOT_ACTIVE: member account is not active");
        }
        Attendance attendance;
        if (sessionId != null) {
            Session session = lockedSession;
            if (recorder.getRole() != null && "Coach".equalsIgnoreCase(recorder.getRole().getName()) && !recorder.getId().equals(assignedCoachId(session))) {
                throw new AccessDeniedException("Coach may only check in assigned sessions");
            }
            Integer classId = session.getSportsClass().getId();
            if (session.getBookedMember() != null) {
                if (!session.getBookedMember().getId().equals(memberId)) {
                    throw new BusinessException("NOT_YOUR_CLASS: member is not the booked member for this private session");
                }
                var entitlement=session.getMemberPackage();
                if(entitlement==null||!"Active".equals(entitlement.getStatus())||!com.sportscenter.membership.EntitlementTimePolicy.validThrough(entitlement,session.getSessionDate().atTime(session.getEndTime())))throw new BusinessException("PACKAGE_EXPIRED: Original entitlement is not valid for this session");
            } else if (!enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(memberId, classId, "Registered")) {
                throw new BusinessException("NOT_YOUR_CLASS: member is not Registered in the class");
            }
            LocalDateTime startsAt = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
            LocalDateTime endsAt = LocalDateTime.of(session.getSessionDate(), session.getEndTime());
            if ("Cancelled".equalsIgnoreCase(session.getStatus()) || now.isBefore(startsAt) || !now.isBefore(endsAt)) {
                throw new BusinessException("CHECKIN_WINDOW_CLOSED: check-in is outside the session window");
            }
            attendance = attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId).orElseGet(Attendance::new);
            if (attendance.getAttendanceId() == null) {
                attendance.setSession(session);
                attendance.setMember(member.getUser());
                attendance.setRecordedBy(recorder);
            }
            attendance.setState("Present");
            if (attendance.getCheckInTime() == null) attendance.setCheckInTime(now);
            trainingResultRepository.findBySessionIdAndMemberId(sessionId, memberId).ifPresent(result -> {
                result.setAttendanceStatus(attendance.getState());
                trainingResultRepository.save(result);
            });
        } else {
            attendance = new Attendance();
            attendance.setMember(member.getUser());
            attendance.setRecordedBy(recorder);
            attendance.setState("CheckedIn");
            attendance.setCheckInTime(now);
        }
        member.setCheckinLastStep(step);
        SCAN_ATTEMPTS.remove(memberId);
        memberRepository.save(member);
        Attendance saved = attendanceRepository.save(attendance);
        auditService.log(recorder.getId(), "SCAN_CHECKIN", "attendances", saved.getAttendanceId(),
                "QR check-in recorded for member " + memberId + (sessionId == null ? " at lobby" : " for session " + sessionId));
        return AttendanceResponse.from(saved);
    }

    private String normalizeState(String state) {
        return SessionAttendancePolicy.state(state);
    }

    private Integer assignedCoachId(Session session) {
        return session.getCoach() != null ? session.getCoach().getId()
                : session.getSportsClass() != null && session.getSportsClass().getCoach() != null ? session.getSportsClass().getCoach().getId() : null;
    }
}
