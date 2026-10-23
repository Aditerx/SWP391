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
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TrainingResultResponse> searchResults(Integer sessionId, Integer memberId, Integer coachId,
                                                      Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_COACH".equals(authority.getAuthority()))) {
            User coach = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated coach account was not found"));
            if (coachId != null && !coachId.equals(coach.getId())) {
                throw new AccessDeniedException("Coaches may only view their own training results");
            }
            coachId = coach.getId();
        }
        return trainingResultRepository.searchResults(sessionId, memberId, coachId).stream()
                .map(TrainingResultResponse::from)
                .toList();
    }

    @Transactional
    public TrainingResultResponse recordResult(TrainingResultRequest request) {
        return recordResult(request, null);
    }

    @Transactional
    public TrainingResultResponse recordResult(TrainingResultRequest request, Authentication authentication) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));

        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        User actor = authentication == null ? null : userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
        boolean coachActor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_COACH".equals(authority.getAuthority()));
        Integer coachId = coachActor ? actor.getId() : request.coachId();
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + coachId));
        if (coachActor) ensureCoachCanRecord(session, member, actor);
        if (actor != null && !coachActor) validateStaffAttendance(session, member, request.content());

        String status = normalizeAttendanceStatus(request.attendanceStatus());

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
        result.setContent(request.content());
        result.setAttendanceStatus(status);
        result.setRecordedAt(LocalDateTime.now(clock));

        TrainingResult saved = trainingResultRepository.save(result);

        // Sync with Attendance record
        syncAttendanceRecord(session, member, coach, status);

        auditService.log(actor != null ? actor.getId() : coach.getId(), "RECORD_RESULT", "training_results", saved.getResultId(),
                "Recorded attendance (" + status + ") and result for member " + member.getFullName() +
                        " in session #" + session.getId() + (correction ? "; previous=" + previousStatus + "; reason=" + request.reason() : ""));

        return TrainingResultResponse.from(saved);
    }

    @Transactional
    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request) {
        return recordBulkResults(request, null);
    }

    @Transactional
    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request, Authentication authentication) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));

        User actor = authentication == null ? null : userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
        boolean coachActor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_COACH".equals(authority.getAuthority()));
        Integer coachId = coachActor ? actor.getId() : request.coachId();
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
                if (item.memberId() == null) continue;

                User member = userRepository.findById(item.memberId()).orElse(null);
                if (member == null) continue;
                if (coachActor) ensureCoachCanRecord(session, member, actor);
                if (actor != null && !coachActor) validateStaffAttendance(session, member, item.content());

                String status = normalizeAttendanceStatus(item.attendanceStatus());

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
                syncAttendanceRecord(session, member, coach, status);

                responses.add(TrainingResultResponse.from(saved));
            }
        }

        auditService.log(actor != null ? actor.getId() : coach.getId(), "RECORD_RESULT", "sessions", session.getId(),
                "Bulk attendance recorded for " + responses.size() + " members in session #" + session.getId());

        return responses;
    }

    private void syncAttendanceRecord(Session session, User member, User recordedBy, String state) {
        Attendance att = attendanceRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(Attendance::new);

        att.setSession(session);
        att.setMember(member);
        att.setRecordedBy(recordedBy);
        att.setState(state);

        if ("Present".equalsIgnoreCase(state) || "Late".equalsIgnoreCase(state)) {
            if (att.getCheckInTime() == null) {
                att.setCheckInTime(LocalDateTime.now(clock));
            }
        }

        attendanceRepository.save(att);
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
        if ("Absent".equalsIgnoreCase(status) || "absent".equalsIgnoreCase(status)) return "Absent";
        if ("Late".equalsIgnoreCase(status) || "late".equalsIgnoreCase(status)) return "Late";
        if ("Excused".equalsIgnoreCase(status) || "excused".equalsIgnoreCase(status)) return "Excused";
        return "Present";
    }

    private void validateStaffAttendance(Session session, User member, String content) {
        if (content != null && !content.isBlank()) throw new AccessDeniedException("Only the assigned coach may write training assessments");
        boolean eligible = session.getBookedMember() != null ? member.getId().equals(session.getBookedMember().getId())
                : enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(member.getId(), session.getSportsClass().getId(), "Registered");
        if (!eligible) throw new BusinessException("NOT_YOUR_CLASS: Member does not belong to this session");
    }
}
