package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));

        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        User coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.coachId()));

        String status = normalizeAttendanceStatus(request.attendanceStatus());

        TrainingResult result = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(TrainingResult::new);

        result.setSession(session);
        result.setMember(member);
        result.setCoach(coach);
        result.setContent(request.content());
        result.setAttendanceStatus(status);
        result.setRecordedAt(LocalDateTime.now());

        TrainingResult saved = trainingResultRepository.save(result);

        // Sync with Attendance record
        syncAttendanceRecord(session, member, coach, status);

        auditService.log(coach.getId(), "RECORD_RESULT", "training_results", saved.getResultId(),
                "Recorded attendance (" + status + ") and result for member " + member.getFullName() +
                        " in session #" + session.getId());

        return TrainingResultResponse.from(saved);
    }

    @Transactional
    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));

        Integer coachId = request.coachId();
        if (coachId == null && session.getSportsClass() != null && session.getSportsClass().getCoach() != null) {
            coachId = session.getSportsClass().getCoach().getId();
        }

        User coach = coachId != null ? userRepository.findById(coachId).orElse(null) : null;
        if (coach == null) {
            throw new BusinessException("Coach is required for recording results");
        }

        List<TrainingResultResponse> responses = new ArrayList<>();

        if (request.items() != null) {
            for (BulkTrainingResultRequest.Item item : request.items()) {
                if (item.memberId() == null) continue;

                User member = userRepository.findById(item.memberId()).orElse(null);
                if (member == null) continue;

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
                tr.setRecordedAt(LocalDateTime.now());

                TrainingResult saved = trainingResultRepository.save(tr);
                syncAttendanceRecord(session, member, coach, status);

                responses.add(TrainingResultResponse.from(saved));
            }
        }

        auditService.log(coach.getId(), "RECORD_RESULT", "sessions", session.getId(),
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
        return "Present";
    }
}
