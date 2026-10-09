package com.sportscenter.specialization;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecializationService {
    private final SpecializationRepository repository;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SpecializationResponse> findAll(String status, Integer subjectId) {
        return repository.findAll().stream()
                .filter(item -> status == null || status.isBlank() || item.getStatus().equalsIgnoreCase(status))
                .filter(item -> subjectId == null || item.getSubject() == null
                        ? subjectId == null : subjectId.equals(item.getSubject().getId()))
                .map(SpecializationResponse::from).toList();
    }

    @Transactional
    public SpecializationResponse create(SpecializationRequest request) {
        repository.findByNameIgnoreCase(request.name().trim()).ifPresent(existing -> {
            throw new BusinessException("Specialization name already exists");
        });
        Specialization item = new Specialization();
        apply(item, request);
        Specialization saved = repository.save(item);
        auditService.log(null, "CREATE_SPECIALIZATION", "SPECIALIZATION", saved.getId(), saved.getName());
        return SpecializationResponse.from(saved);
    }

    @Transactional
    public SpecializationResponse update(Integer id, SpecializationRequest request) {
        Specialization item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found: " + id));
        repository.findByNameIgnoreCase(request.name().trim()).filter(other -> !other.getId().equals(id))
                .ifPresent(existing -> { throw new BusinessException("Specialization name already exists"); });
        apply(item, request);
        return SpecializationResponse.from(repository.save(item));
    }

    @Transactional
    public SpecializationResponse updateStatus(Integer id, String status) {
        Specialization item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found: " + id));
        item.setStatus(normalizeStatus(status));
        return SpecializationResponse.from(repository.save(item));
    }

    private void apply(Specialization item, SpecializationRequest request) {
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setStatus(request.status() == null ? "Active" : normalizeStatus(request.status()));
        if (request.subjectId() == null) {
            item.setSubject(null);
        } else {
            Subject subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
            item.setSubject(subject);
        }
    }

    private String normalizeStatus(String status) {
        if ("Active".equalsIgnoreCase(status)) return "Active";
        if ("Inactive".equalsIgnoreCase(status)) return "Inactive";
        throw new BusinessException("Status must be Active or Inactive");
    }
}
