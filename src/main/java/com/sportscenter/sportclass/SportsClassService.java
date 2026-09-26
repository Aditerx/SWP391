package com.sportscenter.sportclass;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SportsClassService {
    private final SportsClassRepository repository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SportsClassResponse> findAll() {
        return repository.findAll().stream().map(SportsClassResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SportsClassResponse findById(Integer id) {
        return SportsClassResponse.from(getEntity(id));
    }

    @Transactional
    public SportsClassResponse create(SportsClassRequest request) {
        validateNewClassDate(request);
        validateDateRange(request);
        SportsClass sportsClass = new SportsClass();
        apply(sportsClass, request);
        SportsClass saved = repository.save(sportsClass);
        auditService.log(null, "CREATE_CLASS", "CLASS", saved.getId(), saved.getName());
        return SportsClassResponse.from(saved);
    }

    @Transactional
    public SportsClassResponse update(Integer id, SportsClassRequest request) {
        validateDateRange(request);
        SportsClass sportsClass = getEntity(id);
        apply(sportsClass, request);
        return SportsClassResponse.from(repository.save(sportsClass));
    }

    @Transactional
    public SportsClassResponse assignCoach(Integer id, Integer coachId) {
        SportsClass sportsClass = getEntity(id);
        User coach = getCoach(coachId);
        sportsClass.setCoach(coach);
        SportsClass saved = repository.save(sportsClass);
        auditService.log(null, "ASSIGN_COACH", "CLASS", saved.getId(), "coachId=" + coachId);
        return SportsClassResponse.from(saved);
    }

    private void apply(SportsClass sportsClass, SportsClassRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        sportsClass.setName(request.name());
        sportsClass.setSubject(subject);
        sportsClass.setCoach(request.coachId() == null ? null : getCoach(request.coachId()));
        sportsClass.setMaxCapacity(request.maxCapacity());
        sportsClass.setStartDate(request.startDate());
        sportsClass.setEndDate(request.endDate());
        sportsClass.setStatus(request.status() == null ? "ACTIVE" : request.status());
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
                && request.startDate().isBefore(LocalDate.now())) {
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
