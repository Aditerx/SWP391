package com.sportscenter.audit;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Set;

/** Resolve trusted targets inside the event transaction, never from the actor's current center. */
@Component
@RequiredArgsConstructor
public class AuditProvenance {
    private final EntityManager em;
    private static final Set<String> MEMBER_BUSINESS = Set.of("CREATE_MEMBER", "UPDATE_MEMBER", "CHANGE_MEMBER_STATUS");

    public void stamp(SystemLog log) {
        if (log.getTargetEntity() == null || log.getTargetId() == null) return;
        String target = log.getTargetEntity().toUpperCase(Locale.ROOT);
        String query = switch (target) {
            case "USER", "USERS", "MEMBER", "MEMBERS" -> "select e.centerId from User e where e.id=:id";
            case "ROOM", "ROOMS" -> "select e.centerId from Room e where e.id=:id";
            case "CLASS", "CLASSES" -> "select e.centerId from SportsClass e where e.id=:id";
            case "SESSION", "SESSIONS" -> "select e.sportsClass.centerId from Session e where e.id=:id";
            case "INVOICE", "INVOICES" -> "select e.member.centerId from Invoice e where e.invoiceId=:id";
            case "CLASS_ENROLLMENT", "CLASS_ENROLLMENTS" -> "select e.sportsClass.centerId from ClassEnrollment e where e.id=:id";
            case "MEMBER_PACKAGE", "MEMBER_PACKAGES" -> "select e.member.centerId from MemberPackage e where e.subscriptionId=:id";
            case "PRIVATE_SESSION_PROPOSAL" -> "select e.subscription.member.centerId from PrivateSessionProposal e where e.id=:id";
            case "ADVANCED_REGISTRATION", "ADVANCED_REGISTRATIONS" -> "select e.member.centerId from AdvancedRegistration e where e.id=:id";
            case "ATTENDANCE", "ATTENDANCES" -> "select e.session.sportsClass.centerId from Attendance e where e.attendanceId=:id";
            case "TRAINING_RESULTS" -> "select e.session.sportsClass.centerId from TrainingResult e where e.resultId=:id";
            case "TRAINING_PLANS" -> "select e.coach.centerId from TrainingPlan e where e.planId=:id";
            case "EVALUATIONS" -> "select e.coach.centerId from Evaluation e where e.evaluationId=:id";
            case "CLASS_FEEDBACKS" -> "select e.sportsClass.centerId from ClassFeedback e where e.feedbackId=:id";
            default -> null;
        };
        if ("ROLE".equals(target) || "SPECIALIZATION".equals(target)) {
            log.setCategory("SYSTEM_SECURITY");
            log.setScopeSource("GLOBAL_CONFIGURATION_AT_EVENT");
            return;
        }
        if (query == null) return;
        var centers = em.createQuery(query, Integer.class).setParameter("id", log.getTargetId()).getResultList();
        if (centers.isEmpty() || centers.get(0) == null) return;
        log.setCenterId(centers.get(0));
        log.setCategory(("USER".equals(target) || "USERS".equals(target)) && !MEMBER_BUSINESS.contains(log.getAction())
                ? "SYSTEM_SECURITY" : "BUSINESS");
        log.setScopeSource("TARGET_AT_EVENT");
    }
}
