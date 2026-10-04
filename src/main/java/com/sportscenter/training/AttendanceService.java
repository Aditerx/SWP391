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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final TrainingResultRepository trainingResultRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

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
        List<Attendance> all = attendanceRepository.findAllWithDetails();
        return all.stream()
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
    public AttendanceResponse checkIn(Integer memberId, Integer recordedById) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        User recordedBy = recordedById != null ? userRepository.findById(recordedById).orElse(null) : null;

        Attendance att = new Attendance();
        att.setSession(null);
        att.setMember(member);
        att.setRecordedBy(recordedBy);
        att.setState("CheckedIn");
        att.setCheckInTime(LocalDateTime.now());

        Attendance saved = attendanceRepository.save(att);

        auditService.log(recordedBy != null ? recordedBy.getId() : null,
                "RECORD_RESULT", "attendances", saved.getAttendanceId(),
                "Member " + member.getFullName() + " checked in at counter");

        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(Integer memberId) {
        List<Attendance> openCheckIns = attendanceRepository.findOpenCheckInsByMemberId(memberId);
        if (openCheckIns.isEmpty()) {
            throw new BusinessException("No open check-in found for member #" + memberId);
        }

        Attendance att = openCheckIns.get(0);
        att.setCheckOutTime(LocalDateTime.now());
        Attendance saved = attendanceRepository.save(att);

        auditService.log(null, "RECORD_RESULT", "attendances", saved.getAttendanceId(),
                "Member #" + memberId + " checked out");

        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse correctAttendance(AttendanceCorrectionRequest request) {
        Attendance att = attendanceRepository.findById(request.attendanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + request.attendanceId()));

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

        auditService.log(null, "ATTENDANCE_CORRECTION", "attendances", saved.getAttendanceId(),
                "Status: " + oldState + " -> " + newState + " | Reason: " + auditReason);

        return AttendanceResponse.from(saved);
    }

    private String normalizeState(String state) {
        if ("Absent".equalsIgnoreCase(state) || "absent".equalsIgnoreCase(state)) return "Absent";
        if ("Late".equalsIgnoreCase(state) || "late".equalsIgnoreCase(state)) return "Late";
        if ("CheckedIn".equalsIgnoreCase(state) || "checked_in".equalsIgnoreCase(state)) return "CheckedIn";
        return "Present";
    }
}
