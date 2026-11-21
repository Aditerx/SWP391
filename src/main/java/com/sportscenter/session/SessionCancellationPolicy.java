package com.sportscenter.session;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.BusinessConflictException;
import com.sportscenter.user.User;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Objects;

/** Pure guard shared by the read preview and the locked cancellation transaction. */
public final class SessionCancellationPolicy {
    private SessionCancellationPolicy() { }
    public static void require(Session session, User actor, LocalDateTime now, boolean attended) {
        if (actor==null || !ActorPolicy.role(actor,"Member") || !"Active".equalsIgnoreCase(actor.getStatus()))
            throw new AccessDeniedException("CANCELLATION_FORBIDDEN: Active booked Member required");
        if (!session.isPrivateSession() && session.getBookedMember()==null)
            throw new BusinessException("PRIVATE_SESSION_REQUIRED: Chỉ có thể hủy lịch riêng cho ca học 1:1.");
        if (session.getBookedMember()==null || !actor.getId().equals(session.getBookedMember().getId())
                || !Objects.equals(actor.getCenterId(),session.getSportsClass().getCenterId()))
            throw new AccessDeniedException("CANCELLATION_FORBIDDEN: Hội viên chỉ được hủy ca học của chính mình trong cùng trung tâm.");
        if ("Cancelled".equals(session.getStatus())) throw new BusinessException("SESSION_ALREADY_CANCELLED: Original credit has already been returned");
        if ("Completed".equals(session.getStatus())) throw new BusinessException("SESSION_COMPLETED: Buổi học đã hoàn thành, không thể hủy.");
        if (attended) throw new BusinessConflictException("SESSION_ALREADY_ATTENDED: Buổi học đã được điểm danh, không thể hủy hoàn credit.");
        if (session.isAttendanceFinalized() || session.isCenterInvalidated()) throw new BusinessException("SESSION_HISTORY_IMMUTABLE: Finalized sessions cannot be cancelled");
        if (session.getSessionDate()==null || session.getStartTime()==null) throw new BusinessException("SESSION_TIME_REQUIRED: Reconcile the legacy session time");
        if (session.getSessionDate().atTime(session.getStartTime()).isBefore(now.plusHours(12)))
            throw new BusinessException("CANCEL_TOO_LATE: Cancellation requires at least 12 hours notice");
        var sub=session.getMemberPackage();
        if (sub==null || sub.getMember()==null || !actor.getId().equals(sub.getMember().getId()))
            throw new BusinessException("SUBSCRIPTION_LINK_REQUIRED: Cannot refund credit without the original subscription");
        if (!"Active".equals(sub.getStatus()) || sub.getExpiresAt()==null || !sub.getExpiresAt().isAfter(now))
            throw new BusinessException("ADVANCED_PACKAGE_EXPIRED: Expired entitlements cannot be refunded as new credit");
        if (session.isCreditReturned()) throw new BusinessException("CREDIT_ALREADY_RETURNED: Original credit already returned");
        if (sub.getRemainingCredit()!=null && sub.getRemainingCredit()>=12)
            throw new BusinessException("CREDIT_LIMIT_EXCEEDED: Credit refund exceeds the original entitlement");
    }
}
