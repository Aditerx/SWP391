package com.sportscenter.normal;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.enrollment.*;
import com.sportscenter.invoice.*;
import com.sportscenter.session.*;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.sportclass.*;
import com.sportscenter.user.*;
import com.sportscenter.audit.AuditService;
import com.sportscenter.notification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;

/** Manual bank workflow. No bank/provider calls and no public asset URLs. */
@Service @RequiredArgsConstructor
public class NormalRefundService {
    private final NormalRefundRepository refunds;
    private final PaymentResolutionRepository resolutions;
    private final InvoiceRepository invoices;
    private final RefundReceiverRepository receivers;
    private final RefundAttemptRepository attempts;
    private final RefundDocumentRepository documents;
    private final RefundVerificationRepository verifications;
    private final ClassEnrollmentRepository enrollments;
    private final SessionRepository sessions;
    private final HolidayRepository holidays;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final UserRepository users;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundReceiverInput")
    public record ReceiverInput(String bank,String accountNumber,String accountName,long version) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundReview")
    public record Review(long version,String source,String note) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundClaim")
    public record Claim(long version,UUID requestId) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundSubmission")
    public record Submission(String reference,BigDecimal amount,String currency,LocalDateTime performedAt) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundVerification")
    public record Verification(long version,String outcome,String bankSource,String reference,BigDecimal amount,
            Integer receiverId,LocalDateTime performedAt,String note) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundAttemptView")
    public record AttemptView(Integer id,String status,Integer receiverId,BigDecimal amount,String currency,
            String bankReference,LocalDateTime performedAt,Integer claimedBy,LocalDateTime verifiedAt,List<Integer> documentIds) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundView")
    public record View(Integer id,Integer invoiceId,Integer classId,Integer memberId,String origin,String status,
            BigDecimal amount,long version,Integer receiverId,String bank,String accountNumber,String accountName,
            boolean receiverVerified,LocalDateTime dueAt,LocalDateTime firstDueAt,boolean overdue,String issueNote,
            String contactPhone,List<AttemptView> attempts) {}

    /** Called only under the center/class/invoice mutex by a trusted cancellation or Member choice. */
    @Transactional
    public NormalRefund obligation(Invoice invoice,SportsClass c,String origin,String reason,Integer actorId) {
        if(!c.isNormal() || !"Paid".equals(invoice.getPaymentStatus()) || invoice.isCashConfirmationReversed())
            throw new BusinessException("NORMAL_REFUND_NOT_ELIGIBLE: A genuine Normal receipt is required");
        var existing=resolutions.findByInvoiceInvoiceIdAndType(invoice.getInvoiceId(),"Refund");
        if(existing.isPresent()) {
            var result=refunds.findById(existing.get().getId());
            if(result.isPresent()) return result.get();
            throw new BusinessConflictException("REFUND_ALREADY_EXISTS: Reconcile the existing financial resolution");
        }
        if(resolutions.findByInvoiceInvoiceId(invoice.getInvoiceId()).stream().anyMatch(r -> "Completed".equals(r.getStatus())))
            throw new BusinessConflictException("RECEIPT_ALREADY_RESOLVED: Another financial outcome exists");
        PaymentResolution r=new PaymentResolution();r.setInvoice(invoice);r.setType("Refund");r.setStatus("AwaitingReceiver");
        r.setAmount(invoice.getAmount());r.setReason(reason);r.setCreatedAt(now());r.setCreatedBy(actorId==null?invoice.getMember().getId():actorId);
        resolutions.saveAndFlush(r);
        NormalRefund f=new NormalRefund();f.setResolution(r);f.setClassId(c.getId());f.setOrigin(origin);refunds.saveAndFlush(f);
        enrollments.findByMemberIdAndSportsClassId(invoice.getMember().getId(),c.getId()).ifPresent(e -> {
            e.setStatus("Cancelled");e.setCancelledAt(now());e.setCancelReason("Normal refund obligation: "+f.getId());
        });
        invoice.setResolutionStatus("RefundPending");
        log(f,actorId,"OPEN_NORMAL_REFUND","origin="+origin);
        notifyMember(f,"Refund obligation recorded; provide receiving account information");
        return f;
    }

    @Transactional
    public void lateCancelledReceipt(Invoice invoice) {
        SportsClass c=invoice.effectiveClass();
        if(c!=null && c.isNormal() && "Cancelled".equals(c.getStatus()))
            obligation(invoice,c,"LateReceipt","Verified money received after center cancellation",null);
    }

    @Transactional(readOnly=true) public List<View> list(Authentication auth) {
        User actor=actors.actor(auth); readRole(actor,auth);
        var list=ActorPolicy.role(actor,"Member")?refunds.findByResolutionInvoiceMemberIdOrderByIdDesc(actor.getId()):refunds.findByResolutionInvoiceMemberCenterIdOrderByIdDesc(actor.getCenterId());
        return list.stream().filter(f -> !ActorPolicy.role(actor,"Receptionist") || Set.of("Approved","Processing","AwaitingVerification","NeedsReview","Completed").contains(f.getResolution().getStatus())).map(f -> view(f,actor)).toList();
    }
    @Transactional(readOnly=true) public View get(Integer id,Authentication auth) {User actor=actors.actor(auth);readRole(actor,auth);NormalRefund f=entity(id);scope(f,actor);if(ActorPolicy.role(actor,"Receptionist")&&!Set.of("Approved","Processing","AwaitingVerification","NeedsReview","Completed").contains(f.getResolution().getStatus()))throw new AccessDeniedException("Only approved financial work is visible to Receptionist");return view(f,actor);}

    @Transactional public View receiver(Integer id,ReceiverInput input,Authentication auth) {
        User actor=actors.actor(auth);if(!ActorPolicy.role(actor,"Member"))throw new AccessDeniedException("Member confirmation required");
        NormalRefund f=locked(id,actor);version(f,input.version());
        if(!Objects.equals(actor.getId(),f.getResolution().getInvoice().getMember().getId()))throw new AccessDeniedException("Own refund required");
        mutableReceiver(f);text(input.bank(),120);text(input.accountName(),150);
        if(input.accountNumber()==null || !input.accountNumber().matches("[A-Za-z0-9]{4,40}"))throw new BusinessException("RECEIVER_INVALID: Account number must contain 4..40 letters/digits");
        RefundReceiver r=new RefundReceiver();r.setRefundId(id);r.setBank(input.bank().strip());r.setAccountNumber(input.accountNumber());r.setAccountName(input.accountName().strip());r.setConfirmedBy(actor.getId());r.setConfirmedAt(now());receivers.saveAndFlush(r);
        f.setReceiverId(r.getId());f.setApprovedReceiverId(null);f.getResolution().setApprovedAt(null);f.getResolution().setApprovedBy(null);
        f.getResolution().setStatus("AwaitingApproval");f.setVersion(f.getVersion()+1);
        log(f,actor.getId(),"SET_REFUND_RECEIVER","receiverVersion="+r.getId()+"; approval invalidated");return view(f,actor);
    }
    @Transactional public View verifyReceiver(Integer id,Review input,Authentication auth) {
        User actor=manager(auth);NormalRefund f=locked(id,actor);version(f,input.version());mutableReceiver(f);ActorPolicy.reason(input.source());
        RefundReceiver r=receiver(f);if(r.getVerifiedAt()!=null)return view(f,actor);r.setVerifiedBy(actor.getId());r.setVerifiedAt(now());r.setVerificationSource(input.source());f.setVersion(f.getVersion()+1);
        log(f,actor.getId(),"VERIFY_REFUND_RECEIVER","receiverVersion="+r.getId());return view(f,actor);
    }
    @Transactional public View approve(Integer id,Review input,Authentication auth) {
        User actor=manager(auth);NormalRefund f=locked(id,actor);version(f,input.version());mutableReceiver(f);eligible(f);
        RefundReceiver r=receiver(f);if(r.getVerifiedAt()==null)throw new BusinessException("RECEIVER_VERIFICATION_REQUIRED: Verify Member and receiving account first");
        f.setApprovedReceiverId(r.getId());var resolution=f.getResolution();resolution.setApprovedBy(actor.getId());resolution.setApprovedAt(now());resolution.setStatus("Approved");
        f.setDueAt(due(now()));if(f.getFirstDueAt()==null)f.setFirstDueAt(f.getDueAt());f.setVersion(f.getVersion()+1);
        log(f,actor.getId(),"APPROVE_NORMAL_REFUND","approved receiver version="+r.getId()+"; due="+f.getDueAt());notifyMember(f,"Refund approved; transfer target is seven center opening days");return view(f,actor);
    }
    @Transactional public AttemptView claim(Integer id,Claim input,Authentication auth) {
        User actor=cashier(auth);NormalRefund f=locked(id,actor);
        if(input.requestId()==null)throw new BusinessException("REQUEST_ID_REQUIRED: Use a stable UUID for claim retries");
        var old=attempts.findByClaimKey(input.requestId().toString());
        if(old.isPresent()) {RefundAttempt a=old.get();if(!id.equals(a.getRefundId()) || !actor.getId().equals(a.getClaimedBy()))throw new BusinessConflictException("CLAIM_KEY_REUSED");return attemptView(a);}
        version(f,input.version());eligible(f);
        if(!"Approved".equals(f.getResolution().getStatus()) || !Objects.equals(f.getReceiverId(),f.getApprovedReceiverId()))throw new BusinessException("REFUND_APPROVAL_REQUIRED: Current receiver must be approved");
        if(attempts.findByRefundIdOrderByIdDesc(id).stream().anyMatch(a -> !Set.of("Failed","Reversed").contains(a.getStatus())))throw new BusinessConflictException("REFUND_CLAIM_EXISTS: Reconcile the previous bank attempt before any new transfer");
        RefundAttempt a=new RefundAttempt();a.setRefundId(id);a.setReceiverId(f.getApprovedReceiverId());a.setAmount(f.getResolution().getAmount());a.setClaimKey(input.requestId().toString());a.setClaimedBy(actor.getId());a.setClaimedAt(now());attempts.saveAndFlush(a);
        f.getResolution().setStatus("Processing");f.setVersion(f.getVersion()+1);log(f,actor.getId(),"CLAIM_NORMAL_REFUND","attempt="+a.getId());return attemptView(a);
    }
    @Transactional public AttemptView submit(Integer id,Integer attempt,Submission input,Authentication auth) {
        User actor=cashier(auth);NormalRefund f=locked(id,actor);RefundAttempt a=attempt(f,attempt);ownClaim(a,actor);
        if(!Set.of("Claimed","Submitted").contains(a.getStatus()))throw new BusinessException("ATTEMPT_NOT_EDITABLE");
        text(input.reference(),120);
        if(input.amount()==null || input.amount().compareTo(a.getAmount())!=0 || !"VND".equals(input.currency()))throw new BusinessException("REFUND_AMOUNT_MISMATCH: Use the exact approved VND amount");
        if(input.performedAt()==null || input.performedAt().isAfter(now()) || input.performedAt().isBefore(a.getClaimedAt()))throw new BusinessException("REFUND_TIME_INVALID: Transfer must be between claim and now");
        if(attempts.existsByBankReferenceAndIdNot(input.reference().strip(),a.getId()))throw new BusinessConflictException("BANK_REFERENCE_REUSED");
        if(a.getBankReference()!=null && (!a.getBankReference().equals(input.reference().strip()) || !a.getPerformedAt().equals(input.performedAt())))throw new BusinessConflictException("ATTEMPT_IMMUTABLE: Record an issue; bank details cannot be overwritten");
        a.setBankReference(input.reference().strip());a.setPerformedAt(input.performedAt());a.setStatus("Submitted");f.getResolution().setStatus("AwaitingVerification");f.setVersion(f.getVersion()+1);
        log(f,actor.getId(),"SUBMIT_REFUND_TRANSFER","attempt="+a.getId());return attemptView(a);
    }
    @Transactional public Integer upload(Integer id,Integer attempt,MultipartFile file,Authentication auth) {
        User actor=cashier(auth);NormalRefund f=locked(id,actor);RefundAttempt a=attempt(f,attempt);ownClaim(a,actor);
        if(!Set.of("Claimed","Submitted").contains(a.getStatus()))throw new BusinessException("ATTEMPT_NOT_EDITABLE");
        if(file.isEmpty() || file.getSize()>5*1024*1024)throw new BusinessException("DOCUMENT_SIZE_INVALID: Maximum is 5 MiB");
        try {
            byte[] data=file.getBytes();String type=contentType(data);
            if(!type.equals(file.getContentType()))throw new BusinessException("DOCUMENT_TYPE_INVALID: Use a PNG, JPEG or PDF with matching content type");
            String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
            var old=documents.findByAttemptIdOrderByIdAsc(attempt).stream().filter(d -> hash.equals(d.getSha256())).findFirst();if(old.isPresent())return old.get().getId();
            RefundDocument d=new RefundDocument();d.setAttemptId(attempt);d.setContent(data);d.setContentType(type);d.setSha256(hash);d.setUploadedAt(now());d.setUploadedBy(actor.getId());documents.saveAndFlush(d);
            log(f,actor.getId(),"UPLOAD_REFUND_DOCUMENT","attempt="+attempt+"; document="+d.getId());return d.getId();
        } catch(java.io.IOException | java.security.NoSuchAlgorithmException e) {throw new BusinessException("DOCUMENT_READ_FAILED");}
    }
    @Transactional public View verify(Integer id,Integer attempt,Verification input,Authentication auth) {
        User actor=manager(auth);NormalRefund f=locked(id,actor);RefundAttempt a=attempt(f,attempt);
        if("Completed".equals(f.getResolution().getStatus()) && "Verified".equals(a.getStatus()) && "Success".equals(input.outcome()))return view(f,actor);
        version(f,input.version());ActorPolicy.reason(input.bankSource());ActorPolicy.reason(input.note());
        if(!Set.of("Claimed","Submitted").contains(a.getStatus()) && !("Verified".equals(a.getStatus())&&"Reversed".equals(input.outcome())))throw new BusinessException("ATTEMPT_NOT_VERIFIABLE");
        if(input.outcome()==null||!Set.of("Success","Failed","Reversed","Pending").contains(input.outcome()))throw new BusinessException("BANK_OUTCOME_INVALID");
        if("Pending".equals(input.outcome())){f.setIssueNote(input.note());f.getResolution().setStatus("NeedsReview");}
        else if("Success".equals(input.outcome())) {
            eligible(f);
            if(!"Submitted".equals(a.getStatus()) || !documents.existsByAttemptId(attempt))throw new BusinessException("TRANSFER_DOCUMENT_REQUIRED: Submission and private proof are required");
            if(!Objects.equals(a.getReceiverId(),input.receiverId()) || !Objects.equals(a.getBankReference(),input.reference()) || input.amount()==null || a.getAmount().compareTo(input.amount())!=0 || !Objects.equals(a.getPerformedAt(),input.performedAt()))throw new BusinessException("BANK_VERIFICATION_MISMATCH: Independently checked bank details must match");
            a.setStatus("Verified");var r=f.getResolution();r.setStatus("Completed");r.setPerformedAt(a.getPerformedAt());r.setCompletedAt(now());r.setCompletedBy(actor.getId());r.setEvidence("Private attempt "+attempt+"; bank verification retained");r.setOutcomeNote(input.note());r.getInvoice().setResolutionStatus("Completed");
            notifyMember(f,"Refund verified completed: "+r.getAmount()+" VND; reference "+a.getBankReference());
        } else {
            a.setStatus(input.outcome());f.getResolution().setStatus("AwaitingApproval");f.getResolution().getInvoice().setResolutionStatus("RefundPending");f.setApprovedReceiverId(null);f.setIssueNote(input.note());
        }
        a.setVerifiedBy(actor.getId());a.setVerifiedAt(now());a.setBankSource(input.bankSource());a.setOutcomeNote(input.note());f.setVersion(f.getVersion()+1);
        RefundVerification v=new RefundVerification();v.setAttemptId(attempt);v.setVerifiedBy(actor.getId());v.setVerifiedAt(now());v.setOutcome(input.outcome());v.setBankSource(input.bankSource());v.setNote(input.note());verifications.save(v);
        log(f,actor.getId(),"VERIFY_NORMAL_REFUND","attempt="+attempt+"; outcome="+input.outcome());return view(f,actor);
    }
    @Transactional public View issue(Integer id,Review input,Authentication auth) {
        User actor=actors.actor(auth);readRole(actor,auth);NormalRefund f=locked(id,actor);version(f,input.version());ActorPolicy.reason(input.note());
        f.setIssueNote(input.note());f.setVersion(f.getVersion()+1);log(f,actor.getId(),"REPORT_REFUND_ISSUE","Issue recorded; bank history retained");notifyMember(f,"Refund issue recorded; center will follow up");return view(f,actor);
    }
    @Transactional public View requestInformation(Integer id,Review input,Authentication auth) {
        User actor=manager(auth);NormalRefund f=locked(id,actor);version(f,input.version());mutableReceiver(f);ActorPolicy.reason(input.note());
        f.setReceiverId(null);f.setApprovedReceiverId(null);f.setIssueNote(input.note());f.getResolution().setStatus("AwaitingReceiver");f.getResolution().setApprovedAt(null);f.getResolution().setApprovedBy(null);f.setVersion(f.getVersion()+1);
        log(f,actor.getId(),"REQUEST_REFUND_INFORMATION","Correction requested; receiver history retained");notifyMember(f,"Receiving information needs correction; view the case note");return view(f,actor);
    }
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundDocumentDownload")
    public record DocumentDownload(byte[] content,String contentType) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundReceiverHistory")
    public record ReceiverHistory(Integer id,String bank,String accountNumber,String accountName,Integer confirmedBy,LocalDateTime confirmedAt,Integer verifiedBy,LocalDateTime verifiedAt,String verificationSource) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundVerificationHistory")
    public record VerificationHistory(Integer id,Integer attemptId,Integer verifiedBy,LocalDateTime verifiedAt,String outcome,String bankSource,String note) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="NormalRefundHistory")
    public record History(List<ReceiverHistory> receivers,List<VerificationHistory> verifications) {}
    @Transactional(readOnly=true) public History history(Integer id,Authentication auth) {
        User actor=manager(auth);NormalRefund f=entity(id);scope(f,actor);
        return new History(receivers.findByRefundIdOrderByIdAsc(id).stream().map(r -> new ReceiverHistory(r.getId(),r.getBank(),r.getAccountNumber(),r.getAccountName(),r.getConfirmedBy(),r.getConfirmedAt(),r.getVerifiedBy(),r.getVerifiedAt(),r.getVerificationSource())).toList(),attempts.findByRefundIdOrderByIdDesc(id).stream().flatMap(a -> verifications.findByAttemptIdOrderByIdAsc(a.getId()).stream()).map(v -> new VerificationHistory(v.getId(),v.getAttemptId(),v.getVerifiedBy(),v.getVerifiedAt(),v.getOutcome(),v.getBankSource(),v.getNote())).toList());
    }
    @Transactional public DocumentDownload download(Integer id,Integer document,Authentication auth) {
        User actor=actors.actor(auth);staff(actor,auth);NormalRefund f=entity(id);scope(f,actor);
        RefundDocument d=documents.findById(document).orElseThrow(() -> new ResourceNotFoundException("Document not found"));attempt(f,d.getAttemptId());
        audit.log(actor.getId(),"READ_REFUND_DOCUMENT","INVOICE",f.getResolution().getInvoice().getInvoiceId(),"document="+document);
        return new DocumentDownload(d.getContent(),d.getContentType());
    }
    public LocalDateTime due(LocalDateTime approved) {
        LocalDate day=approved.toLocalDate();for(int n=0;n<7;) {day=day.plusDays(1);if(!holidays.existsByHolidayDateAndIsActiveTrue(day))n++;}
        return day.plusDays(1).atStartOfDay().minusNanos(1000);
    }
    @Transactional public void remind() {
        for(NormalRefund f:refunds.findAll()) {
            if("Completed".equals(f.getResolution().getStatus()))continue;
            boolean overdue=f.getDueAt()!=null&&now().isAfter(f.getDueAt());
            boolean waiting="AwaitingReceiver".equals(f.getResolution().getStatus());
            if(!overdue&&!waiting&&!"AwaitingApproval".equals(f.getResolution().getStatus()))continue;
            User member=f.getResolution().getInvoice().getMember();var recipients=new ArrayList<Integer>();recipients.add(member.getId());
            if(overdue||!waiting)users.findAll().stream().filter(u -> ActorPolicy.role(u,"CenterManager")&&"Active".equals(u.getStatus())&&Objects.equals(member.getCenterId(),u.getCenterId())).map(User::getId).forEach(recipients::add);
            events.publishEvent(NotificationTemplates.event(NotificationType.PAYMENT_RECONCILED,recipients,overdue?"Refund transfer target is overdue; obligation remains open. Center must report progress and recorded issues.":waiting?"Refund obligation is waiting for receiving account information; rights are not waived.":"Refund case awaits financial review.","INVOICE",f.getResolution().getInvoice().getInvoiceId(),"NORMAL_REFUND_REMINDER:"+f.getId()+":"+LocalDate.now(clock)+":"+f.getResolution().getStatus()));
        }
    }
    private NormalRefund locked(Integer id,User actor) {
        mutex.acquire(actor.getCenterId());NormalRefund f=entity(id);scope(f,actor);
        invoices.findByIdForUpdate(f.getResolution().getInvoice().getInvoiceId()).orElseThrow();return f;
    }
    private void scope(NormalRefund f,User actor) {Invoice i=f.getResolution().getInvoice();ActorPolicy.center(actor,i.getMember().getCenterId());if(ActorPolicy.role(actor,"Member")&&!actor.getId().equals(i.getMember().getId()))throw new AccessDeniedException("Own refund required");}
    private NormalRefund entity(Integer id){return refunds.findById(id).orElseThrow(() -> new ResourceNotFoundException("Normal refund not found"));}
    private void eligible(NormalRefund f) {
        var r=f.getResolution();Invoice i=r.getInvoice();
        if(!"Paid".equals(i.getPaymentStatus())||i.isCashConfirmationReversed()||i.getAmount().compareTo(r.getAmount())!=0||r.getAmount().signum()<=0)throw new BusinessException("RECEIPT_NOT_ELIGIBLE");
        if(resolutions.findByInvoiceInvoiceId(i.getInvoiceId()).stream().anyMatch(x -> !x.getId().equals(r.getId())&&"Completed".equals(x.getStatus())))throw new BusinessConflictException("RECEIPT_ALREADY_RESOLVED");
        if(enrollments.findByMemberIdAndSportsClassId(i.getMember().getId(),f.getClassId()).map(e -> !"Cancelled".equals(e.getStatus())).orElse(false))throw new BusinessConflictException("NORMAL_RIGHT_STILL_ACTIVE");
        if(!Objects.equals(i.effectiveClass().getId(),f.getClassId()))throw new BusinessConflictException("NORMAL_RIGHT_TRANSFERRED");
    }
    private void mutableReceiver(NormalRefund f) {
        if("Completed".equals(f.getResolution().getStatus()) || attempts.findByRefundIdOrderByIdDesc(f.getId()).stream().anyMatch(a -> !Set.of("Failed","Reversed").contains(a.getStatus())))throw new BusinessConflictException("REFUND_BANK_ATTEMPT_EXISTS: Reconcile bank outcome before changing receiver or approval");
    }
    private RefundReceiver receiver(NormalRefund f){if(f.getReceiverId()==null)throw new BusinessException("RECEIVER_REQUIRED");return receivers.findById(f.getReceiverId()).orElseThrow();}
    private RefundAttempt attempt(NormalRefund f,Integer id){var a=attempts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));if(!f.getId().equals(a.getRefundId()))throw new AccessDeniedException("Attempt belongs to another refund");return a;}
    private void ownClaim(RefundAttempt a,User actor){if(!actor.getId().equals(a.getClaimedBy()))throw new AccessDeniedException("Claimed Receptionist required");}
    private void version(NormalRefund f,long version){if(f.getVersion()!=version)throw new BusinessConflictException("REFUND_VERSION_STALE: Reload the current case");}
    private User manager(Authentication auth){User u=actors.actor(auth);if(!ActorPolicy.role(u,"CenterManager"))throw new AccessDeniedException("CenterManager required");authority(auth,"MANAGE_INVOICES");return u;}
    private User cashier(Authentication auth){User u=actors.actor(auth);if(!ActorPolicy.role(u,"Receptionist"))throw new AccessDeniedException("Receptionist required");authority(auth,"MANAGE_INVOICES");return u;}
    private void readRole(User u,Authentication auth){if(!ActorPolicy.role(u,"Member"))staff(u,auth);}
    private void staff(User u,Authentication auth){if(!ActorPolicy.role(u,"CenterManager")&&!ActorPolicy.role(u,"Receptionist"))throw new AccessDeniedException("Financial staff required");authority(auth,"MANAGE_INVOICES");}
    private void authority(Authentication auth,String permission){if(auth.getAuthorities().stream().noneMatch(a -> permission.equals(a.getAuthority())))throw new AccessDeniedException(permission+" required");}
    private void text(String text,int max){if(text==null||text.isBlank()||text.length()>max)throw new BusinessException("REFUND_INPUT_INVALID: Required text exceeds allowed length");}
    private String contentType(byte[] data){if(data.length>4&&data[0]=='%'&&data[1]=='P'&&data[2]=='D'&&data[3]=='F'&&data[4]=='-')return "application/pdf";if(data.length>8&&data[0]==(byte)137&&data[1]==80&&data[2]==78&&data[3]==71&&data[4]==13&&data[5]==10&&data[6]==26&&data[7]==10)return "image/png";if(data.length>3&&data[0]==(byte)255&&data[1]==(byte)216&&data[2]==(byte)255)return "image/jpeg";throw new BusinessException("DOCUMENT_TYPE_INVALID: PNG, JPEG or PDF required");}
    private AttemptView attemptView(RefundAttempt a){return new AttemptView(a.getId(),a.getStatus(),a.getReceiverId(),a.getAmount(),a.getCurrency(),a.getBankReference(),a.getPerformedAt(),a.getClaimedBy(),a.getVerifiedAt(),documents.findByAttemptIdOrderByIdAsc(a.getId()).stream().map(RefundDocument::getId).toList());}
    private View view(NormalRefund f,User actor) {
        var r=f.getResolution();boolean staff=!ActorPolicy.role(actor,"Member");RefundReceiver receiver=f.getReceiverId()==null?null:receiver(f);
        boolean full=ActorPolicy.role(actor,"CenterManager") || ActorPolicy.role(actor,"Receptionist")&&f.getApprovedReceiverId()!=null;
        String number=receiver==null?null:receiver.getAccountNumber();if(number!=null&&!full)number="****"+number.substring(Math.max(0,number.length()-4));
        return new View(f.getId(),r.getInvoice().getInvoiceId(),f.getClassId(),r.getInvoice().getMember().getId(),f.getOrigin(),r.getStatus(),r.getAmount(),f.getVersion(),f.getReceiverId(),receiver==null?null:receiver.getBank(),number,receiver==null?null:receiver.getAccountName(),receiver!=null&&receiver.getVerifiedAt()!=null,f.getDueAt(),f.getFirstDueAt(),f.getDueAt()!=null&&now().isAfter(f.getDueAt())&&!"Completed".equals(r.getStatus()),f.getIssueNote(),staff?r.getInvoice().getMember().getPhone():null,attempts.findByRefundIdOrderByIdDesc(f.getId()).stream().map(this::attemptView).toList());
    }
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    private void log(NormalRefund f,Integer actor,String action,String detail){audit.log(actor,action,"INVOICE",f.getResolution().getInvoice().getInvoiceId(),"refund="+f.getId()+"; "+detail);}
    private void notifyMember(NormalRefund f,String message){events.publishEvent(NotificationTemplates.event(NotificationType.PAYMENT_RECONCILED,List.of(f.getResolution().getInvoice().getMember().getId()),message,"INVOICE",f.getResolution().getInvoice().getInvoiceId(),"NORMAL_REFUND:"+f.getId()+":"+f.getVersion()+":"+f.getResolution().getStatus()));}
}
