package com.sportscenter.invoice;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.session.ScheduleWriteLock;
import com.sportscenter.audit.AuditService;
import com.sportscenter.notification.*;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentResolutionService {
    private final PaymentResolutionRepository resolutions;
    private final InvoiceRepository invoices;
    private final MemberPackageRepository subscriptions;
    private final SessionRepository sessions;
    private final com.sportscenter.membership.SubscriptionPauseRepository pauses;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final com.sportscenter.enrollment.ClassEnrollmentRepository enrollments;
    @io.swagger.v3.oas.annotations.media.Schema(name="PaymentResolutionResponse")
    public record Response(Integer id,Integer invoiceId,String type,String status,BigDecimal amount,String reason,String evidence,
            String outcomeNote,LocalDateTime createdAt,LocalDateTime approvedAt,LocalDateTime performedAt,LocalDateTime completedAt) {
        static Response from(PaymentResolution r){return new Response(r.getId(),r.getInvoice().getInvoiceId(),r.getType(),r.getStatus(),r.getAmount(),r.getReason(),r.getEvidence(),r.getOutcomeNote(),r.getCreatedAt(),r.getApprovedAt(),r.getPerformedAt(),r.getCompletedAt());}
    }
    @Transactional
    public Response open(Integer invoiceId,String type,String reason,Authentication auth){
        User actor=actors.actor(auth);requireStaff(actor);ActorPolicy.reason(reason);mutex.acquire(actor.getCenterId());
        Invoice invoice=lock(invoiceId);ActorPolicy.center(actor,invoice.getMember().getCenterId());
        if(!Set.of("Refund","CashCorrection").contains(type))throw new BusinessException("RESOLUTION_TYPE_INVALID: Use Refund or CashCorrection");
        if(!"Paid".equals(invoice.getPaymentStatus()))throw new BusinessException("PAID_INVOICE_REQUIRED: Resolve a recorded Paid invoice");
        var existing=resolutions.findByInvoiceInvoiceIdAndType(invoiceId,type);
        if(existing.isPresent())return Response.from(existing.get());
        PaymentResolution r=new PaymentResolution();r.setInvoice(invoice);r.setType(type);r.setAmount(invoice.getAmount());r.setReason(reason);r.setCreatedBy(actor.getId());r.setCreatedAt(LocalDateTime.now(clock));resolutions.save(r);
        audit.log(actor.getId(),"OPEN_PAYMENT_RESOLUTION","INVOICE",invoiceId,type+"; reason="+reason);
        return Response.from(r);
    }
    @Transactional
    public Response approve(Integer id,Authentication auth){
        User actor=actors.actor(auth);ActorPolicy.manager(actor);mutex.acquire(actor.getCenterId());
        PaymentResolution r=entity(id);Invoice invoice=lock(r.getInvoice().getInvoiceId());ActorPolicy.center(actor,invoice.getMember().getCenterId());
        if("Completed".equals(r.getStatus())||"Approved".equals(r.getStatus()))return Response.from(r);
        String blocked=eligibility(r,invoice);
        if(blocked!=null){r.setStatus("Blocked");r.setOutcomeNote(blocked);audit.log(actor.getId(),"BLOCK_PAYMENT_RESOLUTION","INVOICE",invoice.getInvoiceId(),"case="+id+"; "+blocked);return Response.from(r);}
        r.setStatus("Approved");r.setApprovedBy(actor.getId());r.setApprovedAt(LocalDateTime.now(clock));r.setOutcomeNote(null);
        audit.log(actor.getId(),"APPROVE_PAYMENT_RESOLUTION","INVOICE",invoice.getInvoiceId(),"case="+id+"; approved only, no refund performed");
        return Response.from(r);
    }
    @Transactional
    public Response complete(Integer id,String evidence,LocalDateTime performedAt,boolean moneyNotReceived,Authentication auth){
        User actor=actors.actor(auth);ActorPolicy.manager(actor);ActorPolicy.reason(evidence);mutex.acquire(actor.getCenterId());
        PaymentResolution r=entity(id);Invoice invoice=lock(r.getInvoice().getInvoiceId());ActorPolicy.center(actor,invoice.getMember().getCenterId());
        if("Completed".equals(r.getStatus()))return Response.from(r);
        if(!"Approved".equals(r.getStatus()))throw new BusinessException("RESOLUTION_APPROVAL_REQUIRED: Manager must approve the case first");
        String blocked=eligibility(r,invoice);
        if(blocked!=null){r.setStatus("Blocked");r.setOutcomeNote(blocked);audit.log(actor.getId(),"BLOCK_PAYMENT_RESOLUTION","INVOICE",invoice.getInvoiceId(),"case="+id+"; "+blocked);return Response.from(r);}
        if("Refund".equals(r.getType())){
            if(moneyNotReceived)throw new BusinessException("REFUND_RECEIPT_REQUIRED: A refund cannot represent money never received");
            if(performedAt==null||performedAt.isAfter(LocalDateTime.now(clock))||invoice.getPaymentDate()==null||performedAt.isBefore(invoice.getPaymentDate()))
                throw new BusinessException("REFUND_TIME_INVALID: Actual external refund time must be between receipt and now");
            r.setPerformedAt(performedAt);
        }else{
            if(!moneyNotReceived)throw new BusinessException("CASH_NOT_RECEIVED_CONFIRMATION_REQUIRED: Verify that no money was received");
            var sub=subscriptions.findByIdForUpdate(invoice.getSubscription().getSubscriptionId()).orElseThrow();
            sub.setStatus("Cancelled");sub.setRemainingCredit(0);
            invoice.setCashConfirmationReversed(true);
        }
        r.setEvidence(evidence);r.setCompletedBy(actor.getId());r.setCompletedAt(LocalDateTime.now(clock));r.setStatus("Completed");r.setOutcomeNote("Refund".equals(r.getType())?"Full refund performed outside SCMS":"Mistaken Cash confirmation voided; no receipt or refund occurred");
        invoice.setResolutionStatus("Completed");
        audit.log(actor.getId(),"COMPLETE_PAYMENT_RESOLUTION","INVOICE",invoice.getInvoiceId(),"case="+id+"; type="+r.getType()+"; original amount retained="+invoice.getAmount()+"; evidence="+evidence);
        events.publishEvent(NotificationTemplates.event(NotificationType.PAYMENT_RECONCILED,List.of(invoice.getMember().getId()),r.getOutcomeNote(),"INVOICE",invoice.getInvoiceId(),"PAYMENT_RESOLUTION:"+id));
        return Response.from(r);
    }
    @Transactional(readOnly=true) public List<Response> list(Authentication auth){User actor=actors.actor(auth);requireStaff(actor);return resolutions.findByInvoiceMemberCenterId(actor.getCenterId()).stream().map(Response::from).toList();}
    private String eligibility(PaymentResolution r,Invoice invoice){
        if(!"Paid".equals(invoice.getPaymentStatus())||invoice.isCashConfirmationReversed())return "Original receipt is no longer eligible";
        if(resolutions.findByInvoiceInvoiceId(invoice.getInvoiceId()).stream().anyMatch(x->!x.getId().equals(r.getId())&&"Completed".equals(x.getStatus())))return "Another financial resolution was already completed";
        if("Refund".equals(r.getType())){
            if(!invoice.isReconciliationRequired())return "MVP refund applies only to money received without granting an entitlement";
            if(invoice.getSubscription()!=null&&List.of("Active","Completed").contains(invoice.getSubscription().getStatus()))return "Entitlement was granted; reconcile dependencies before an external refund";
            return null;
        }
        if(!"Cash".equals(invoice.getPaymentMethod())||invoice.getSportsClass()!=null||invoice.getSubscription()==null||!invoice.getSubscription().isAdvanced())return "Automatic Cash correction is limited to unused Advanced subscriptions";
        var sub=invoice.getSubscription();
        if(!"Active".equals(sub.getStatus())||sub.getSubject()==null||sub.getPrimaryCoach()==null||sub.getActivatedAt()==null||!Integer.valueOf(12).equals(sub.getRemainingCredit()))return "Original Advanced entitlement is missing, inactive, or not wholly unused";
        if(sessions.existsByMemberPackageSubscriptionId(sub.getSubscriptionId()))return "Booking history exists, including cancelled/refunded sessions";
        if(enrollments.existsByEligibilitySubscriptionSubscriptionId(sub.getSubscriptionId()))return "Another class registration depends on this subscription";
        if(enrollments.findByMemberIdWithDetails(sub.getMember().getId()).stream().anyMatch(e->e.getEligibilitySubscription()==null&&(e.getEnrolledAt()==null||!e.getEnrolledAt().isBefore(sub.getActivatedAt()))))return "Legacy class-registration dependency requires reconciliation";
        if(invoices.findBySubscriptionSubscriptionId(sub.getSubscriptionId()).size()!=1)return "Another invoice depends on this entitlement";
        if(!pauses.findBySubscriptionSubscriptionId(sub.getSubscriptionId()).isEmpty()||sub.getApprovedExtraSeconds()!=0||sub.getApprovedExtraNanos()!=0||sub.getExpiryProcessedAt()!=null||sub.getDeferredCreditReturn()!=0)return "Extension, expiry, incident, or recovery history exists";
        return null;
    }
    private void requireStaff(User actor){if(!ActorPolicy.role(actor,"Receptionist"))ActorPolicy.manager(actor);}
    private Invoice lock(Integer id){return invoices.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Invoice not found"));}
    private PaymentResolution entity(Integer id){return resolutions.findById(id).orElseThrow(()->new ResourceNotFoundException("Payment resolution not found"));}
}
