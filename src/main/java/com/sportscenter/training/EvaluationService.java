package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
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
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EvaluationResponse> searchEvaluations(Integer memberId, Integer coachId) {
        return evaluationRepository.searchEvaluations(memberId, coachId).stream()
                .map(EvaluationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResponse findById(Integer id) {
        Evaluation ev = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));
        return EvaluationResponse.from(ev);
    }

    @Transactional
    public EvaluationResponse createEvaluation(EvaluationRequest request) {
        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        User coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.coachId()));

        if (request.progressScore() != null) {
            if (request.progressScore().compareTo(BigDecimal.ZERO) < 0 || request.progressScore().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessException("Progress score must be between 0.0 and 10.0");
            }
        }

        Evaluation ev = new Evaluation();
        ev.setMember(member);
        ev.setCoach(coach);
        ev.setEvaluationDate(request.evaluationDate() != null ? request.evaluationDate() : LocalDate.now());
        ev.setComment(request.comment());
        ev.setProgressScore(request.progressScore());

        Evaluation saved = evaluationRepository.save(ev);

        auditService.log(coach.getId(), "RECORD_RESULT", "evaluations", saved.getEvaluationId(),
                "Coach " + coach.getFullName() + " evaluated member " + member.getFullName() +
                        " (Score: " + saved.getProgressScore() + "/10)");

        return EvaluationResponse.from(saved);
    }

    @Transactional
    public EvaluationResponse updateEvaluation(Integer id, EvaluationRequest request) {
        Evaluation ev = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));

        if (request.comment() != null) {
            ev.setComment(request.comment());
        }
        if (request.progressScore() != null) {
            if (request.progressScore().compareTo(BigDecimal.ZERO) < 0 || request.progressScore().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessException("Progress score must be between 0.0 and 10.0");
            }
            ev.setProgressScore(request.progressScore());
        }
        if (request.evaluationDate() != null) {
            ev.setEvaluationDate(request.evaluationDate());
        }

        Evaluation saved = evaluationRepository.save(ev);

        auditService.log(ev.getCoach() != null ? ev.getCoach().getId() : null,
                "RECORD_RESULT", "evaluations", saved.getEvaluationId(),
                "Updated evaluation for member #" + (ev.getMember() != null ? ev.getMember().getId() : "N/A"));

        return EvaluationResponse.from(saved);
    }

    @Transactional
    public void deleteEvaluation(Integer id) {
        Evaluation ev = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));

        evaluationRepository.delete(ev);

        auditService.log(ev.getCoach() != null ? ev.getCoach().getId() : null,
                "RECORD_RESULT", "evaluations", id,
                "Deleted evaluation #" + id);
    }
}
