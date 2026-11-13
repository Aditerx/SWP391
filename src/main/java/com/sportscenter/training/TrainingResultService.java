package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingResultService {
    private final TrainingResultRepository trainingResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final com.sportscenter.invoice.InvoiceRepository invoiceRepository;
    private final Clock clock;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    @Transactional(readOnly = true)
    public List<TrainingResultResponse> searchResults(Integer sessionId, Integer memberId, Integer coachId,
                                                      Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Authenticated account required");
        }
        User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account was not found"));
        if (!"Active".equalsIgnoreCase(actor.getStatus()) || actor.getRole() == null) {
            throw new AccessDeniedException("Active account with a role required");
        }
        Integer centerId = null;
        if (com.sportscenter.common.ActorPolicy.role(actor, "Member")) {
            if (memberId != null && !memberId.equals(actor.getId())) {
                throw new AccessDeniedException("Members may only view their own training results");
            }
            memberId = actor.getId();
        } else if (com.sportscenter.common.ActorPolicy.role(actor, "Coach")) {
            if (coachId != null && !coachId.equals(actor.getId())) {
                throw new AccessDeniedException("Coaches may only view their own training results");
            }
            coachId = actor.getId();
        } else {
            if (authentication.getAuthorities().stream().noneMatch(a ->
                    "MANAGE_CLASSES".equals(a.getAuthority()) || "MANAGE_USERS".equals(a.getAuthority()))) {
                throw new AccessDeniedException("Training results read permission required");
            }
            if (actor.getCenterId() == null && !com.sportscenter.common.ActorPolicy.role(actor, "Admin")) {
                throw new AccessDeniedException("Account center required");
            }
            centerId = actor.getCenterId();
        }
        return trainingResultRepository.searchResults(sessionId, memberId, coachId, centerId).stream()
                .map(tr -> TrainingResultResponse.from(tr, attendanceRepository.findBySessionIdAndMemberId(tr.getSession().getId(), tr.getMember().getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public TrainingResultResponse recordResult(TrainingResultRequest request) {
        return recordResult(request, null);
    }

    @Transactional
    public TrainingResultResponse recordResult(TrainingResultRequest request, Authentication authentication) {
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(request.sessionId()).orElseThrow(()->new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));
        SessionAttendancePolicy.requireWindow(session,LocalDateTime.now(clock));

        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        User actor = requireRecorder(authentication,session);
        requireContentWriter(actor, authentication, request.content());
        boolean coachActor = com.sportscenter.common.ActorPolicy.role(actor, "Coach");
        User assigned=session.getCoach()!=null?session.getCoach():session.getSportsClass().getCoach();
        Integer coachId = coachActor ? actor.getId() : assigned==null?null:assigned.getId();
        if(coachId==null)throw new BusinessException("COACH_REQUIRED: Assigned Coach required");
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + coachId));
        if (coachActor) ensureCoachCanRecord(session, member, actor);
        if (!coachActor) validateStaffAttendance(session, member);

        String status = normalizeAttendanceStatus(request.attendanceStatus());
        SessionAttendancePolicy.note(request.note());

        TrainingResult result = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(TrainingResult::new);

        Attendance previousAttendance = attendanceRepository.findBySessionIdAndMemberId(session.getId(), member.getId()).orElse(null);
        String previousStatus = previousAttendance != null ? previousAttendance.getState() : result.getAttendanceStatus();
        boolean correction = previousStatus != null && !previousStatus.equals(status);
        if (correction && session.getSessionDate() != null
                && session.getSessionDate().isBefore(LocalDate.now(clock))
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException("A reason is required to correct attendance for a past session");
        }

        result.setSession(session);
        result.setMember(member);
        result.setCoach(coach);
        if (request.content() != null) result.setContent(request.content());
        result.setAttendanceStatus(status);
        result.setRecordedAt(LocalDateTime.now(clock));

        TrainingResult saved = trainingResultRepository.save(result);

        // Sync with Attendance record
        Attendance attendance = syncAttendanceRecord(session, member, actor, status, request.note());

        auditService.log(actor != null ? actor.getId() : coach.getId(), "RECORD_RESULT", "training_results", saved.getResultId(),
                "Recorded attendance (" + status + ") and result for member " + member.getFullName() +
                        " in session #" + session.getId() + (correction ? "; previous=" + previousStatus + "; reason=" + request.reason() : ""));

        return TrainingResultResponse.from(saved, attendance);
    }

    @Transactional
    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request) {
        return recordBulkResults(request, null);
    }

    @Transactional
    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request, Authentication authentication) {
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(request.sessionId()).orElseThrow(()->new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));
        SessionAttendancePolicy.requireWindow(session,LocalDateTime.now(clock));

        User actor = requireRecorder(authentication,session);
        boolean coachActor = com.sportscenter.common.ActorPolicy.role(actor, "Coach");
        User assigned=session.getCoach()!=null?session.getCoach():session.getSportsClass().getCoach();
        Integer coachId = coachActor ? actor.getId() : assigned==null?null:assigned.getId();
        if (coachId == null && session.getSportsClass() != null && session.getSportsClass().getCoach() != null) {
            coachId = session.getSportsClass().getCoach().getId();
        }

        if (coachId == null) {
            throw new BusinessException("Coach is required for recording results");
        }

        final Integer resolvedCoachId = coachId;
        User coach = userRepository.findById(resolvedCoachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + resolvedCoachId));

        List<TrainingResultResponse> responses = new ArrayList<>();

        if (request.items() != null) {
            for (BulkTrainingResultRequest.Item item : request.items()) {
                requireContentWriter(actor, authentication, item.content());
                if (item.memberId() == null) continue;

                User member = userRepository.findById(item.memberId()).orElse(null);
                if (member == null) continue;
                if (coachActor) ensureCoachCanRecord(session, member, actor);
                if (!coachActor) validateStaffAttendance(session, member);

                String status = normalizeAttendanceStatus(item.attendanceStatus());
                SessionAttendancePolicy.note(item.note());

                TrainingResult tr = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                        .orElseGet(TrainingResult::new);

                tr.setSession(session);
                tr.setMember(member);
                tr.setCoach(coach);
                if (item.content() != null) {
                    tr.setContent(item.content());
                }
                tr.setAttendanceStatus(status);
                tr.setRecordedAt(LocalDateTime.now(clock));

                TrainingResult saved = trainingResultRepository.save(tr);
                Attendance attendance = syncAttendanceRecord(session, member, actor, status, item.note());

                responses.add(TrainingResultResponse.from(saved, attendance));
            }
        }

        auditService.log(actor != null ? actor.getId() : coach.getId(), "RECORD_RESULT", "sessions", session.getId(),
                "Bulk attendance recorded for " + responses.size() + " members in session #" + session.getId());

        return responses;
    }

    @Transactional
    public TrainingResultResponse updateContent(Integer resultId, TrainingResultContentRequest request,
                                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName()))
            throw new AccessDeniedException("RESULT_CONTENT_FORBIDDEN: Active assigned Coach required");
        User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("RESULT_CONTENT_FORBIDDEN: Account not found"));
        if (!"Active".equalsIgnoreCase(actor.getStatus()) || !com.sportscenter.common.ActorPolicy.role(actor, "Coach")
                || !SessionAttendancePolicy.permission(actor, "RECORD_RESULT")
                || authentication.getAuthorities().stream().noneMatch(a -> "RECORD_RESULT".equals(a.getAuthority()))
                || actor.getCenterId() == null)
            throw new AccessDeniedException("RESULT_CONTENT_FORBIDDEN: Active Coach with RECORD_RESULT required");
        if (request == null || request.content() == null)
            throw new BusinessException("RESULT_CONTENT_REQUIRED: Supply non-null content");

        // Same order as manual, correction and finalizer. Resolve only a scalar target before row locks;
        // never carry an unlocked TrainingResult snapshot across a concurrent attendance correction.
        scheduleWriteLock.acquire(actor.getCenterId());
        Integer sessionId = trainingResultRepository.findSessionIdByResultId(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Training result not found: " + resultId));
        com.sportscenter.common.ActorPolicy.center(actor, sessionRepository.findCenterIdById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        TrainingResult result = trainingResultRepository.findByIdForUpdate(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Training result not found: " + resultId));
        com.sportscenter.common.ActorPolicy.center(actor, session.getSportsClass().getCenterId());
        if (!sessionId.equals(result.getSession().getId()) || !actor.getId().equals(SessionAttendancePolicy.coachId(session))
                || result.getCoach() == null || !actor.getId().equals(result.getCoach().getId()))
            throw new AccessDeniedException("RESULT_CONTENT_FORBIDDEN: Only the assigned Coach and result author may edit content");
        com.sportscenter.common.ActorPolicy.center(actor, result.getMember().getCenterId());

        LocalDateTime now = SessionAttendancePolicy.now(clock);
        requireContentWindow(session, now);
        Attendance attendance = attendanceRepository.findBySessionIdAndMemberId(sessionId, result.getMember().getId()).orElse(null);
        // The current-attendance lookup deliberately hides superseded history. Its absence must
        // not turn a superseded-only record into the allowed "no Attendance was ever created" case.
        if (attendance == null && attendanceRepository.existsBySessionIdAndMemberIdAndSupersededAtIsNotNull(sessionId, result.getMember().getId()))
            throw new BusinessException("ATTENDANCE_SUPERSEDED: Preserve superseded attendance history");
        requireContentEligibility(session, result.getMember());
        if (java.util.Objects.equals(result.getContent(), request.content()))
            return TrainingResultResponse.from(result, attendance);

        result.setContent(request.content());
        trainingResultRepository.save(result);
        auditService.log(actor.getId(), "UPDATE_RESULT_CONTENT", "training_results", result.getResultId(),
                "sessionId=" + sessionId + "; memberId=" + result.getMember().getId()
                        + "; change=" + (request.content().isEmpty() ? "CLEARED" : "UPDATED"), now);
        return TrainingResultResponse.from(result, attendance);
    }

    private void requireContentWindow(Session session, LocalDateTime now) {
        if (session.getSportsClass().isNormal() && session.getSportsClass().getMinCapacity() != null
                && !java.util.Set.of("Ongoing", "Closed").contains(session.getSportsClass().getStatus()))
            throw new BusinessException("NORMAL_NOT_STARTED: Manager-confirmed opening is required");
        if (session.getSessionDate() == null || session.getStartTime() == null || session.getEndTime() == null)
            throw new BusinessException("SESSION_TIME_REQUIRED: Reconcile the legacy session time");
        if (session.isCenterInvalidated() || !("Scheduled".equals(session.getStatus()) || "Completed".equals(session.getStatus())))
            throw new BusinessException("SESSION_NOT_HELD: Preserve incident and result history");
        if (now.isBefore(session.getSessionDate().atTime(session.getEndTime())))
            throw new BusinessException("RESULT_CONTENT_NOT_OPEN: Use in-session recording before session end");
        if (!now.isBefore(SessionAttendancePolicy.cutoff(session)))
            throw new AccessDeniedException("RESULT_CONTENT_DEADLINE_PASSED: Đã hết hạn sửa nhận xét buổi học của Coach.");
    }

    private void requireContentEligibility(Session session, User member) {
        boolean eligible;
        if (session.getBookedMember() != null) {
            var entitlement = session.getMemberPackage();
            eligible = member.getId().equals(session.getBookedMember().getId()) && entitlement != null && entitlement.isAdvanced()
                    && entitlement.getMember() != null && member.getId().equals(entitlement.getMember().getId())
                    && java.util.Objects.equals(session.getSportsClass().getSubject().getId(), entitlement.getSubject() == null ? null : entitlement.getSubject().getId())
                    && ("Active".equals(entitlement.getStatus()) || "Completed".equals(entitlement.getStatus()) || "Expired".equals(entitlement.getStatus()))
                    && com.sportscenter.membership.EntitlementTimePolicy.validThrough(entitlement, session.getSessionDate().atTime(session.getStartTime()))
                    && com.sportscenter.membership.EntitlementTimePolicy.validThrough(entitlement, session.getSessionDate().atTime(session.getEndTime()))
                    && invoiceRepository.existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(entitlement.getSubscriptionId(), "Paid");
        } else {
            eligible = session.getSportsClass().isNormal()
                    && enrollmentRepository.findByMemberIdAndSportsClassId(member.getId(), session.getSportsClass().getId())
                    .filter(e -> ("Registered".equals(e.getStatus()) || "Completed".equals(e.getStatus()))
                            && e.getEnrolledAt() != null && !e.getEnrolledAt().isAfter(session.getSessionDate().atTime(session.getStartTime())))
                    .isPresent() && invoiceRepository.existsByMemberIdAndSportsClassIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(member.getId(), session.getSportsClass().getId(), "Paid");
        }
        if (!eligible) throw new BusinessException("NOT_YOUR_CLASS: No paid eligibility for this session");
    }

    private Attendance syncAttendanceRecord(Session session, User member, User recordedBy, String state, String note) {
        Attendance att = attendanceRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(Attendance::new);
        String oldNote = att.getNote();
        if (note != null) att.setNote(SessionAttendancePolicy.note(note));

        att.setSession(session);
        att.setMember(member);
        att.setRecordedBy(recordedBy);
        att.setState(state);

        if ("Present".equalsIgnoreCase(state) || "Late".equalsIgnoreCase(state)) {
            if (att.getCheckInTime() == null) {
                att.setCheckInTime(LocalDateTime.now(clock));
            }
        }

        Attendance saved = attendanceRepository.save(att);
        if (!java.util.Objects.equals(oldNote, att.getNote()))
            auditService.log(recordedBy.getId(), "ATTENDANCE_NOTE", "attendances", saved.getAttendanceId(), SessionAttendancePolicy.noteAudit(oldNote, att.getNote()));
        return saved;
    }

    private void ensureCoachCanRecord(Session session, User member, User coach) {
        User assigned = session.getCoach() != null ? session.getCoach()
                : session.getSportsClass() != null ? session.getSportsClass().getCoach() : null;
        boolean eligible = session.getBookedMember() != null ? member.getId().equals(session.getBookedMember().getId())
                : session.getSportsClass() != null && enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(member.getId(), session.getSportsClass().getId(), "Registered");
        if (assigned == null || !coach.getId().equals(assigned.getId()) || !eligible) {
            throw new BusinessException("NOT_YOUR_CLASS: coach may only record results for registered members in their own class");
        }
    }

    private String normalizeAttendanceStatus(String status) {
        return SessionAttendancePolicy.state(status);
    }

    private void requireContentWriter(User actor, Authentication authentication, String content) {
        if (content != null && (!com.sportscenter.common.ActorPolicy.role(actor, "Coach")
                || authentication.getAuthorities().stream().noneMatch(a -> "RECORD_RESULT".equals(a.getAuthority())))) {
            throw new AccessDeniedException("Only the assigned Coach with RECORD_RESULT may write training assessments");
        }
    }

    private void validateStaffAttendance(Session session, User member) {
        boolean eligible = session.getBookedMember() != null ? member.getId().equals(session.getBookedMember().getId())
                : enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(member.getId(), session.getSportsClass().getId(), "Registered");
        if (!eligible) throw new BusinessException("NOT_YOUR_CLASS: Member does not belong to this session");
    }
    private User requireRecorder(Authentication auth,Session session){
        if(auth==null||!auth.isAuthenticated())throw new AccessDeniedException("Authentication required");
        User actor=userRepository.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new AccessDeniedException("Account not found"));
        if(!"Active".equals(actor.getStatus()))throw new AccessDeniedException("Active recorder required");
        com.sportscenter.common.ActorPolicy.center(actor,session.getSportsClass().getCenterId());
        if(auth.getAuthorities().stream().noneMatch(a->java.util.Set.of("ROLE_COACH","RECORD_RESULT","MANAGE_CLASSES").contains(a.getAuthority())))throw new AccessDeniedException("Attendance permission required");
        return actor;
    }
}
