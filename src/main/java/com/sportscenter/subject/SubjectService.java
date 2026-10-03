package com.sportscenter.subject;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository repository;
    private final AuditService auditService;

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
        Subject subject = new Subject();
        apply(subject, request);
        Subject saved = repository.save(subject);
        auditService.log("CREATE", "Subject", saved.getId(), "Created subject " + saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse update(Integer id, SubjectRequest request) {
        Subject subject = getEntity(id);
        apply(subject, request);
        Subject saved = repository.save(subject);
        auditService.log("UPDATE", "Subject", saved.getId(), "Updated subject " + saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Subject subject = getEntity(id);
        repository.delete(subject);
        auditService.log("DELETE", "Subject", id, "Deleted subject " + subject.getName());
    }

    private Subject getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setName(request.name());
        subject.setDescription(request.description());
    }
}
