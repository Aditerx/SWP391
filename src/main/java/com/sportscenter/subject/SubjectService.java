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
        auditService.log(null, "CREATE_SUBJECT", "SUBJECT", saved.getId(), saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse update(Integer id, SubjectRequest request) {
        Subject subject = getEntity(id);
        apply(subject, request);
        return SubjectResponse.from(repository.save(subject));
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(getEntity(id));
    }

    private Subject getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setName(request.name());
        subject.setDescription(request.description());
        subject.setStatus(request.status() == null ? "Active" : request.status());
        subject.setSlug(request.slug() == null || request.slug().isBlank()
                ? slugify(request.name()) : slugify(request.slug()));
    }

    private String slugify(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
