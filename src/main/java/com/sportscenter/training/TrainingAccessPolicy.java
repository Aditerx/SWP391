package com.sportscenter.training;

import com.sportscenter.common.AccountScope;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class TrainingAccessPolicy {
    private final UserRepository users;
    private final ClassEnrollmentRepository enrollments;
    private final MemberPackageRepository subscriptions;
    private final InvoiceRepository invoices;

    public User actor() { return AccountScope.actor(users); }

    public User write(User coach, SportsClass sportsClass, User member, boolean plan) {
        User actor = actor();
        if (!ActorPolicy.role(actor, "Coach"))
            throw new AccessDeniedException("Only the assigned Coach may write professional training content");
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        if (authorities.stream().noneMatch(a -> (plan ? "MANAGE_TRAINING_PLAN" : "RECORD_RESULT").equals(a.getAuthority())))
            throw new AccessDeniedException("Training write permission required");
        if (coach == null || !ActorPolicy.role(coach, "Coach") || !"Active".equalsIgnoreCase(coach.getStatus()))
            throw new AccessDeniedException("Active assigned Coach required");
        ActorPolicy.center(actor, coach.getCenterId());
        if (!actor.getId().equals(coach.getId()))
            throw new AccessDeniedException("Only the assigned Coach may write this record");
        if (sportsClass != null) {
            ActorPolicy.center(actor, sportsClass.getCenterId());
            if (sportsClass.getCoach() == null || !coach.getId().equals(sportsClass.getCoach().getId()))
                throw new AccessDeniedException("Coach is not assigned to this class");
        }
        if (member != null) {
            ActorPolicy.center(actor, member.getCenterId());
            if (!ActorPolicy.role(member, "Member") || !assigned(coach, member))
                throw new AccessDeniedException("Member is not assigned to this Coach through a paid course");
        }
        return actor;
    }

    private boolean assigned(User coach, User member) {
        boolean normal = enrollments.findByMemberIdWithDetails(member.getId()).stream().anyMatch(e ->
                "Registered".equals(e.getStatus()) && e.getSportsClass() != null && e.getSportsClass().isNormal()
                && e.getSportsClass().getCoach() != null && coach.getId().equals(e.getSportsClass().getCoach().getId())
                && java.util.Objects.equals(coach.getCenterId(), e.getSportsClass().getCenterId())
                && invoices.existsByMemberIdAndSportsClassIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(
                        member.getId(), e.getSportsClass().getId(), "Paid"));
        return normal || subscriptions.findByMemberId(member.getId()).stream().anyMatch(s ->
                s.isAdvanced() && s.getSubject() != null && s.getPrimaryCoach() != null
                && coach.getId().equals(s.getPrimaryCoach().getId()) && s.getActivatedAt() != null
                && !"Cancelled".equals(s.getStatus())
                && invoices.existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(
                        s.getSubscriptionId(), "Paid"));
    }

    public boolean readable(User actor, User coach, SportsClass sportsClass, User member) {
        return (coach == null || AccountScope.visible(actor, coach.getCenterId()))
                && (sportsClass == null || AccountScope.visible(actor, sportsClass.getCenterId()))
                && (member == null || AccountScope.visible(actor, member.getCenterId()));
    }

    public boolean memberCanReadClassPlan(User actor, SportsClass sportsClass) {
        return ActorPolicy.role(actor, "Member") && sportsClass != null && sportsClass.isNormal()
                && AccountScope.visible(actor, sportsClass.getCenterId())
                && enrollments.existsByMemberIdAndSportsClassIdAndStatus(actor.getId(), sportsClass.getId(), "Registered")
                && invoices.existsByMemberIdAndSportsClassIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(
                        actor.getId(), sportsClass.getId(), "Paid");
    }
}
