package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
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

        if (session.getSportsClass() == null || session.getSportsClass().getCoach() == null
                || !session.getSportsClass().getCoach().getId().equals(request.coachId())) {
            throw new BusinessException("Coach must be assigned to this session's class");
        }
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_COACH".equals(a.getAuthority()))) {
            User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Coach account not found"));
            if (!actor.getId().equals(request.coachId())) {
                throw new AccessDeniedException("Coaches may only record attendance for their own classes");
            }
        }
        if (!enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(
                request.memberId(), session.getSportsClass().getId(), List.of("Registered", "Completed"))) {
            throw new BusinessException("Attendance requires a Registered or Completed enrollment in this class");
        }

        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        User coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.coachId()));

        String status = normalizeAttendanceStatus(request.attendanceStatus());

        TrainingResult result = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(TrainingResult::new);

        Attendance previousAttendance = attendanceRepository.findBySessionIdAndMemberId(session.getId(), member.getId()).orElse(null);
        String previousStatus = previousAttendance != null ? previousAttendance.getState() : result.getAttendanceStatus();
        boolean correction = previousStatus != null && !previousStatus.equals(status);
        if (correction && session.getSessionDate() != null
                && session.getSessionDate().isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException("A reason is required to correct attendance for a past session");
        }

        result.setSession(session);
        result.setMember(member);
        result.setCoach(coach);
        result.setContent(request.content());
        result.setAttendanceStatus(status);
        result.setRecordedAt(LocalDateTime.now());

        TrainingResult saved = trainingResultRepository.save(result);

        // Sync with Attendance record
        syncAttendanceRecord(session, member, coach, status);

        auditService.log(coach.getId(), correction ? "ATTENDANCE_CORRECTION" : "RECORD_RESULT", "training_results", saved.getResultId(),
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

        Integer coachId = request.coachId();
        if (coachId == null && session.getSportsClass() != null && session.getSportsClass().getCoach() != null) {
            coachId = session.getSportsClass().getCoach().getId();
        }

        if (coachId == null) {
            throw new BusinessException("Coach is required for recording results");
        }

        List<TrainingResultResponse> responses = new ArrayList<>();

        if (request.items() != null) {
            for (BulkTrainingResultRequest.Item item : request.items()) {
                responses.add(recordResult(new TrainingResultRequest(request.sessionId(), item.memberId(), coachId,
                        item.content(), item.attendanceStatus(), item.reason()), authentication));
            }
        }

        auditService.log(coachId, "RECORD_RESULT", "sessions", session.getId(),
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
                att.setCheckInTime(LocalDateTime.now());
            }
        }

        attendanceRepository.save(att);
    }

    private String normalizeAttendanceStatus(String status) {
        if ("Absent".equalsIgnoreCase(status) || "absent".equalsIgnoreCase(status)) return "Absent";
        if ("Late".equalsIgnoreCase(status) || "late".equalsIgnoreCase(status)) return "Late";
        if (status == null || "Present".equalsIgnoreCase(status)) return "Present";
        throw new BusinessException("Attendance status must be Present, Absent or Late");
    }
}
