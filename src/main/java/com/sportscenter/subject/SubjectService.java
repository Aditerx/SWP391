package com.sportscenter.subject;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository repository;
    private final AuditService auditService;
    private final com.sportscenter.session.SessionRepository sessions;
    private final com.sportscenter.sportclass.SportsClassRepository classes;
    private final com.sportscenter.specialization.SpecializationRepository specializations;
    private final java.time.Clock clock;

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return repository.findAll().stream().map(SubjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Integer id) {
        return SubjectResponse.from(getEntity(id));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (request.outdoor() == null) throw new com.sportscenter.common.exception.BusinessException("OUTDOOR_REQUIRED: Specify whether the subject is outdoor");
        Subject subject = new Subject();
        apply(subject, request);
        Subject saved = repository.save(subject);
        auditService.log(null, "CREATE_SUBJECT", "SUBJECT", saved.getId(), saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse update(Integer id, SubjectRequest request) {
        Subject subject = getEntity(id);
        if (request.outdoor() != null && request.outdoor() != subject.isOutdoor()
                && sessions.existsBySportsClassSubjectIdAndStatusAndSessionDateGreaterThanEqual(id, "Scheduled", java.time.LocalDate.now(clock))) {
            throw new com.sportscenter.common.exception.BusinessException("SUBJECT_HAS_SCHEDULE: Resolve future sessions before changing indoor/outdoor policy");
        }
        apply(subject, request);
        auditService.log(null, "UPDATE_SUBJECT", "SUBJECT", id, "Updated subject configuration");
        return SubjectResponse.from(repository.save(subject));
    }

    @Transactional
    public void delete(Integer id) {
        if (classes.existsBySubjectId(id) || specializations.existsBySubjectId(id)) {
            throw new com.sportscenter.common.exception.BusinessConflictException("SUBJECT_IN_USE: Subject is referenced by classes or specializations; deactivate it instead");
        }
        repository.delete(getEntity(id));
        auditService.log(null, "DELETE_SUBJECT", "SUBJECT", id, "Deleted unused subject");
    }

    private Subject getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setName(request.name());
        subject.setDescription(request.description());
        if (request.status() != null) subject.setStatus(request.status());
        if (request.outdoor() != null) subject.setOutdoor(request.outdoor());
        subject.setSlug(request.slug() == null || request.slug().isBlank()
                ? slugify(request.name()) : slugify(request.slug()));
        if (request.normalPrice() != null) {
            subject.setNormalPrice(request.normalPrice());
        }
        if (request.advancedPrice() != null) {
            subject.setAdvancedPrice(request.advancedPrice());
        }
    }

    private String slugify(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
