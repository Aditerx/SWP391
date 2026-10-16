package com.sportscenter.report;

import com.sportscenter.center.CenterContext;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.training.AttendanceRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChurnRiskService {
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final CenterContext centerContext;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AtRiskMemberResponse> forCoach(Authentication authentication) {
        User coach = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated coach account was not found"));
        Integer centerId = centerContext.currentCenterId();
        return filter(enrollmentRepository.findRiskCandidatesForCoach(coach.getId(), centerId));
    }

    @Transactional(readOnly = true)
    public List<AtRiskMemberResponse> forCenter() {
        Integer centerId = centerContext.currentCenterId();
        return filter(enrollmentRepository.findRiskCandidatesByCenter(centerId));
    }

    private List<AtRiskMemberResponse> filter(List<ClassEnrollment> candidates) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(14);
        LinkedHashMap<Integer, AtRiskMemberResponse> atRisk = new LinkedHashMap<>();
        for (ClassEnrollment enrollment : candidates) {
            User member = enrollment.getMember();
            if (member != null && !attendanceRepository.hasRecentAttendance(member.getId(), cutoff, now)) {
                atRisk.putIfAbsent(member.getId(), new AtRiskMemberResponse(
                        member.getId(), member.getFullName(), member.getEmail()));
            }
        }
        return List.copyOf(atRisk.values());
    }
}
