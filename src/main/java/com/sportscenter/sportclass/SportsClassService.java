package com.sportscenter.sportclass;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.enrollment.EnrollmentService;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportsClassService {
    private final SportsClassRepository repository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AuditService auditService;
    private final EnrollmentService enrollmentService;
    private final java.time.Clock clock;
    private final com.sportscenter.session.SessionRepository sessionRepository;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    @Transactional(readOnly = true)
    public List<SportsClassResponse> findAll() {
        var counts = classEnrollmentRepository.countGroupedByClass(List.of("Registered", "Pending")).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).intValue()));
        return repository.findAll().stream().map(c -> {
            int count = counts.getOrDefault(c.getId(), 0);
            return SportsClassResponse.from(c, count);
        }).toList();
    }

    @Transactional(readOnly = true)
    public SportsClassResponse findById(Integer id) {
        SportsClass entity = getEntity(id);
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(entity.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(entity, count);
    }

    @Transactional
    public SportsClassResponse create(SportsClassRequest request) {
        validateNewClassDate(request);
        validateDateRange(request);
        SportsClass sportsClass = new SportsClass();
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            sportsClass.setCenterId(userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow().getCenterId());
        }
        apply(sportsClass, request);
        // The legacy two-request setup remains closed until the schedule is complete.
        if (sportsClass.isNormal() && "Open".equals(sportsClass.getStatus())) sportsClass.setStatus("Closed");
        SportsClass saved = repository.save(sportsClass);
        auditService.log(null, "CREATE_CLASS", "CLASS", saved.getId(), saved.getName());
        return SportsClassResponse.from(saved, 0);
    }

    @Transactional
    public SportsClassResponse update(Integer id, SportsClassRequest request, String actorEmail) {
        validateDateRange(request);
        scheduleWriteLock.acquire(repository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
        Integer previousCapacity = sportsClass.getMaxCapacity();
        BigDecimal previousFee = sportsClass.getTuitionFee();
        long occupied = classEnrollmentRepository.countBySportsClassIdAndStatusIn(id, List.of("Registered", "Pending"));
        if (request.maxCapacity() != null && request.maxCapacity() < occupied) throw new BusinessException("CLASS_CAPACITY_EXCEEDED: Cannot reduce capacity below reserved seats");
        if (sessionRepository.countBySportsClassIdAndStatusNot(id, "Cancelled") > 0
                && (!java.util.Objects.equals(request.subjectId(), sportsClass.getSubject().getId())
                || request.classType() != null && !request.classType().equalsIgnoreCase(sportsClass.getClassType())
                || !java.util.Objects.equals(request.coachId(), sportsClass.getCoach() == null ? null : sportsClass.getCoach().getId()))) {
            throw new BusinessException("CLASS_HAS_SCHEDULE: Use per-session incident handling to change the assigned coach; the subject/type is fixed once scheduled");
        }
        if (sportsClass.isNormal() && "Open".equalsIgnoreCase(request.status())) {
            com.sportscenter.session.NormalSchedulePolicy.requireReady(sportsClass, sessionRepository, clock);
        }
        apply(sportsClass, request);
        // Updating metadata does not silently reprice a previously created class.
        if (previousFee != null) sportsClass.setTuitionFee(previousFee);
        SportsClass saved = repository.save(sportsClass);
        if (saved.getMaxCapacity() != null && (previousCapacity == null || saved.getMaxCapacity() > previousCapacity)) {
            Integer actorId = userRepository.findByEmailIgnoreCase(actorEmail).map(User::getId).orElse(null);
            enrollmentService.promoteWaitlist(saved.getId(), actorId);
        }
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(saved.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(saved, count);
    }

    @Transactional
    public SportsClassResponse assignCoach(Integer id, Integer coachId) {
        scheduleWriteLock.acquire(repository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = repository.findByIdForUpdate(id).orElseThrow();
        if (sessionRepository.countBySportsClassIdAndStatusNot(id, "Cancelled") > 0) throw new BusinessException("CLASS_HAS_SCHEDULE: Use substitute-coach for individual scheduled sessions to preserve history");
        User coach = getCoach(coachId);
        sportsClass.setCoach(coach);
        SportsClass saved = repository.save(sportsClass);
        auditService.log(null, "ASSIGN_COACH", "CLASS", saved.getId(), "coachId=" + coachId);
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(saved.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(saved, count);
    }

    private void apply(SportsClass sportsClass, SportsClassRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        if (!"Active".equalsIgnoreCase(subject.getStatus())) throw new BusinessException("SUBJECT_INACTIVE: Cannot create or publish classes for an inactive subject");
        sportsClass.setName(request.name());
        sportsClass.setSubject(subject);
        sportsClass.setCoach(request.coachId() == null ? null : getCoach(request.coachId()));

        String classType = (request.classType() != null && !request.classType().isBlank())
                ? request.classType()
                : (sportsClass.getClassType() != null ? sportsClass.getClassType() : "Normal");

        if ("Normal".equalsIgnoreCase(classType)) {
            if (request.maxCapacity() != null && request.maxCapacity() > 15) {
                throw new BusinessException("Lớp học nhóm Normal chỉ có sức chứa tối đa 15 học viên (maxCapacity <= 15).");
            }
            sportsClass.setClassType("Normal");
            if (request.totalSessions() != null && request.totalSessions() != 12) throw new BusinessException("NORMAL_SESSION_COUNT: Normal classes require exactly 12 sessions");
            sportsClass.setTotalSessions(12);
            // Tự động gán mức giá chuẩn từ Subject.normal_price (Freeze price logic)
            if (subject.getNormalPrice() != null && subject.getNormalPrice().compareTo(BigDecimal.ZERO) > 0) {
                sportsClass.setTuitionFee(subject.getNormalPrice());
            } else if (request.tuitionFee() != null) {
                sportsClass.setTuitionFee(request.tuitionFee());
            } else {
                sportsClass.setTuitionFee(BigDecimal.ZERO);
            }
        } else if ("Advanced".equalsIgnoreCase(classType)) {
            if (request.maxCapacity() != null && request.maxCapacity() != 1) {
                throw new BusinessException("Khóa học 1:1 Advanced bắt buộc có sức chứa bằng 1 (maxCapacity = 1).");
            }
            sportsClass.setClassType("Advanced");
            sportsClass.setTotalSessions(12);
            if (subject.getAdvancedPrice() != null && subject.getAdvancedPrice().compareTo(BigDecimal.ZERO) > 0) {
                sportsClass.setTuitionFee(subject.getAdvancedPrice());
            } else if (request.tuitionFee() != null) {
                sportsClass.setTuitionFee(request.tuitionFee());
            } else {
                sportsClass.setTuitionFee(BigDecimal.ZERO);
            }
        } else {
            throw new BusinessException("Loại lớp học không hợp lệ: " + classType + ". Chỉ chấp nhận 'Normal' hoặc 'Advanced'.");
        }

        sportsClass.setMaxCapacity(request.maxCapacity());
        sportsClass.setStartDate(request.startDate());
        sportsClass.setEndDate(request.endDate());
        sportsClass.setStatus(request.status() == null ? "Open" : normalizeClassStatus(request.status()));
    }

    private String normalizeClassStatus(String status) {
        if ("Open".equalsIgnoreCase(status)) return "Open";
        if ("Ongoing".equalsIgnoreCase(status)) return "Ongoing";
        if ("Closed".equalsIgnoreCase(status)) return "Closed";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        throw new BusinessException("Invalid class status: " + status + ". Must be Open, Ongoing, Closed, or Cancelled");
    }

    private SportsClass getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
    }

    private User getCoach(Integer coachId) {
        User user = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach user not found: " + coachId));
        if (user.getRole() == null || user.getRole().getName() == null
                || !"Coach".equalsIgnoreCase(user.getRole().getName())) {
            throw new BusinessException("User " + coachId + " does not have the Coach role");
        }
        return user;
    }

    private void validateNewClassDate(SportsClassRequest request) {
        if ("Open".equalsIgnoreCase(request.status())
                && request.startDate() != null
                && request.startDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("A new Open class cannot start in the past");
        }
    }

    private void validateDateRange(SportsClassRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Class end date cannot be before its start date");
        }
    }
}
