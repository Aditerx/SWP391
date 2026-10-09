package com.sportscenter.feedback;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassFeedbackService {
    private final ClassFeedbackRepository feedbackRepository;
    private final SportsClassRepository sportsClassRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public ClassFeedbackResponse create(Integer pathClassId, ClassFeedbackRequest request,
                                        Authentication authentication) {
        if (!pathClassId.equals(request.classId())) {
            throw new BusinessException("classId in the request body must match the path classId");
        }

        User member = currentUser(authentication, "Member");
        SportsClass sportsClass = sportsClassRepository.findById(pathClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + pathClassId));

        User coach = sportsClass.getCoach();
        if (coach == null) {
            throw new BusinessException("Cannot submit feedback because this class has no assigned coach");
        }
        if (!coach.getId().equals(request.coachId())) {
            throw new BusinessException("coachId must match the coach assigned to this class");
        }
        if (!feedbackableStatus(sportsClass.getStatus())) {
            throw new AccessDeniedException("Feedback is allowed only for Ongoing or Closed classes");
        }
        if (!enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(
                member.getId(), pathClassId, List.of("Registered", "Completed"))) {
            throw new AccessDeniedException("You must be registered or have completed this class to submit feedback");
        }
        if (!feedbackRepository.hasPresentAttendance(pathClassId, member.getId())) {
            throw new AccessDeniedException("You must have at least one Present attendance in this class to submit feedback");
        }
        if (feedbackRepository.existsBySportsClassIdAndMemberId(pathClassId, member.getId())) {
            throw new ClassFeedbackConflictException("Feedback has already been submitted for this class");
        }

        ClassFeedback feedback = new ClassFeedback();
        feedback.setSportsClass(sportsClass);
        feedback.setMember(member);
        feedback.setCoach(coach);
        feedback.setClassRating(request.classRating());
        feedback.setCoachRating(request.coachRating());
        feedback.setComment(request.comment());

        // Flush here so a concurrent duplicate hits the unique constraint before returning 201.
        ClassFeedback saved;
        try {
            saved = feedbackRepository.saveAndFlush(feedback);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateFeedbackConstraint(exception)) {
                throw new ClassFeedbackConflictException("Feedback has already been submitted for this class");
            }
            throw exception;
        }

        auditService.log(member.getId(), "CREATE_CLASS_FEEDBACK", "class_feedbacks", saved.getFeedbackId(),
                "Submitted feedback for class #" + pathClassId + " and coach #" + coach.getId());
        return ClassFeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ClassFeedbackResponse findMine(Integer classId, Authentication authentication) {
        User member = currentUser(authentication, "Member");
        if (!sportsClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found: " + classId);
        }
        return feedbackRepository.findForMemberAndClass(classId, member.getId())
                .map(ClassFeedbackResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for class #" + classId));
    }

    @Transactional(readOnly = true)
    public ClassFeedbackSummaryResponse findForClass(Integer classId) {
        if (!sportsClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found: " + classId);
        }
        List<ClassFeedback> feedback = feedbackRepository.findForClass(classId);
        return new ClassFeedbackSummaryResponse(
                classId,
                feedback.size(),
                average(feedback.stream().map(ClassFeedback::getClassRating).toList()),
                average(feedback.stream().map(ClassFeedback::getCoachRating).toList()),
                feedback.stream().map(ClassFeedbackReviewResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public CoachFeedbackSummaryResponse findForCurrentCoach(Integer classId, Authentication authentication) {
        User coach = currentUser(authentication, "Coach");
        List<ClassFeedback> feedback = feedbackRepository.findForCoach(coach.getId(), classId);
        return new CoachFeedbackSummaryResponse(
                feedback.size(),
                average(feedback.stream().map(ClassFeedback::getCoachRating).toList()),
                feedback.stream().map(CoachFeedbackReviewResponse::from).toList()
        );
    }

    private User currentUser(Authentication authentication, String roleLabel) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated " + roleLabel.toLowerCase() + " account was not found"));
    }

    private boolean feedbackableStatus(String status) {
        return "Ongoing".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status);
    }

    private BigDecimal average(List<Short> ratings) {
        if (ratings.isEmpty()) return null;
        double average = ratings.stream().mapToInt(Short::intValue).average().orElseThrow();
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isDuplicateFeedbackConstraint(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "uq_feedback_class_member".equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
