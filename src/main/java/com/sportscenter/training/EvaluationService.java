package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    public List<EvaluationResponse> searchEvaluations(Integer memberId, Integer coachId, Authentication authentication) {
        User currentUser = null;
        boolean memberRole = hasRole(authentication, "ROLE_MEMBER");
        boolean coachRole = hasRole(authentication, "ROLE_COACH");
        if (memberRole || coachRole) {
            currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
        }
        if (memberRole) {
            if (memberId != null && !memberId.equals(currentUser.getId())) {
                throw new AccessDeniedException("Members may only view their own evaluations");
            }
            memberId = currentUser.getId();
        } else if (coachRole) {
            if (coachId != null && !coachId.equals(currentUser.getId())) {
                throw new AccessDeniedException("Coaches may only view their own evaluations");
            }
            coachId = currentUser.getId();
        }
        return evaluationRepository.searchEvaluations(memberId, coachId).stream()
                .map(EvaluationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResponse findById(Integer id, Authentication authentication) {
        Evaluation ev = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));
        if (hasRole(authentication, "ROLE_MEMBER") && !ev.getMember().getId().equals(currentUserId(authentication))) {
            throw new AccessDeniedException("Members may only view their own evaluations");
        }
        if (hasRole(authentication, "ROLE_COACH") && !ev.getCoach().getId().equals(currentUserId(authentication))) {
            throw new AccessDeniedException("Coaches may only view their own evaluations");
        }
        return EvaluationResponse.from(ev);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private Integer currentUserId(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
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
