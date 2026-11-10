package com.sportscenter.invoice;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.notification.NotificationTemplates;
import com.sportscenter.notification.NotificationType;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final java.time.Clock clock;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final SportsClassRepository sportsClassRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AuditService auditService;
    private final PaymentProperties paymentProperties;
    private final VnPayGateway vnPayGateway;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    private final ApplicationEventPublisher eventPublisher;
    private final com.sportscenter.membership.EntitlementTimePolicy entitlementTimePolicy;
    private final com.sportscenter.membership.CoachCapacityService coachCapacityService;
    private final com.sportscenter.user.MemberRepository memberRepository;
    private final VnPayPaymentAttemptRepository paymentAttemptRepository;
    private final com.sportscenter.session.SessionRepository normalSessions;
    private final com.sportscenter.normal.NormalRefundService normalRefunds;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public InvoiceOrderResponse createAdvancedOrder(MemberPackage subscription, String method, String clientIp) {
        if (subscription.getSubject() == null || subscription.getPrimaryCoach() == null) throw new BusinessException("SUBJECT_COACH_REQUIRED: Approved registration required");
        Invoice invoice = newInvoice(subscription.getMember(), subscription.getPurchaseAmount()!=null?subscription.getPurchaseAmount():subscription.getSubject().getAdvancedPrice(), method);
        invoice.setSubscription(subscription);
        return orderResponse(invoiceRepository.save(invoice), clientIp);
    }

    @Transactional
    public InvoiceOrderResponse existingOrder(Invoice invoice, String clientIp) {
        scheduleWriteLock.acquire(invoice.getMember().getCenterId());
        invoiceRepository.findSportsClassIdByInvoiceId(invoice.getInvoiceId()).ifPresent(sportsClassRepository::findByIdForUpdate);
        entityManager.refresh(invoice, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!"Pending".equals(invoice.getPaymentStatus()) || expired(invoice))
            return new InvoiceOrderResponse(invoice.getInvoiceId(),invoice.getInvoiceCode(),invoice.getAmount(),invoice.getPaymentMethod(),invoice.getPaymentStatus(),invoice.getExpiresAt(),null);
        return orderResponse(invoice,clientIp);
    }

    /** Browser Return verifies the protocol only. Verified IPN/QueryDR are payment writers. */
    @Transactional(readOnly = true)
    public VnPayResult inspectVnpayReturn(Map<String,String> params) {
        if (!vnPayGateway.isValid(params)) return new VnPayResult("97", "Invalid signature", null);
        Invoice invoice = invoiceRepository.findByInvoiceCode(params.get("vnp_TxnRef")).orElse(null);
        return new VnPayResult(invoice == null ? "01" : "00", invoice == null ? "Order not found" : "Read invoice status through the authenticated API", invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(Integer memberId, Integer packageId, Integer receptionistId,
            String status, String method, String keyword, LocalDateTime startDate, LocalDateTime endDate,
            Authentication authentication) {
        Integer scopedMemberId = resolveMemberScope(authentication, memberId);
        User actor = currentUser(authentication);
        List<Invoice> list = invoiceRepository.searchInvoices(scopedMemberId, packageId, receptionistId,
                blankToNull(status), blankToNull(method), blankToNull(keyword), startDate, endDate);
        return list.stream().filter(i -> java.util.Objects.equals(actor.getCenterId(), i.getMember().getCenterId())).map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findOwn(Authentication authentication) {
        Integer memberId = currentUser(authentication).getId();
        return invoiceRepository.findByMemberIdOrderByInvoiceIdDesc(memberId).stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findOwnById(Integer id, Authentication authentication) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (!invoice.getMember().getId().equals(currentUser(authentication).getId())) {
            throw new ResourceNotFoundException("Invoice not found: " + id);
        }
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Integer id, Authentication authentication) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        Integer scopedMemberId = resolveMemberScope(authentication, null);
        User actor=currentUser(authentication);
        if(!java.util.Objects.equals(actor.getCenterId(),invoice.getMember().getCenterId()))throw new AccessDeniedException("Invoice belongs to another center");
        if (scopedMemberId != null && !scopedMemberId.equals(invoice.getMember().getId())) {
            throw new ResourceNotFoundException("Invoice not found: " + id);
        }
        return InvoiceResponse.from(invoice);
    }
    @Transactional(readOnly=true)
    public InvoiceResponse receipt(Integer id,Authentication auth){
        InvoiceResponse response=findById(id,auth);
        if(!"Paid".equals(response.paymentStatus()))throw new InvoiceConflictException("A receipt is available after payment is recorded");
        return response;
    }

    @Transactional
    public InvoiceOrderResponse createPackageOrder(User member, MembershipPackage pkg, String method, MemberPackage subscription, String clientIp) {
        if (!"Active".equalsIgnoreCase(pkg.getStatus())) throw new BusinessException("Membership package is not Active");
        cancelPendingPackageOrders(member.getId(), pkg.getId());
        Invoice invoice = newInvoice(member, pkg.getPrice(), method);
        invoice.setMembershipPackage(pkg);
        invoice.setSubscription(subscription);
        return orderResponse(invoiceRepository.save(invoice), clientIp);
    }

    @Transactional
    public InvoiceOrderResponse createClassOrder(User member, SportsClass sportsClass, String method, String clientIp) {
        scheduleWriteLock.acquire(member.getCenterId());
        sportsClass = sportsClassRepository.findByIdForUpdate(sportsClass.getId()).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        com.sportscenter.common.ActorPolicy.center(com.sportscenter.common.AccountScope.actor(userRepository), sportsClass.getCenterId());
        com.sportscenter.sportclass.NormalClassPolicy.requireAdmission(sportsClass, normalSessions, clock);
        if ("Cash".equalsIgnoreCase(method)) {
            User cashier = com.sportscenter.common.AccountScope.actor(userRepository);
            if (!com.sportscenter.common.ActorPolicy.role(cashier, "Receptionist")) throw new AccessDeniedException("NORMAL_CASH_COUNTER_ONLY: Receptionist required");
        }
        final Integer orderClassId = sportsClass.getId();
        if (invoiceRepository.findByMemberId(member.getId()).stream().anyMatch(i -> i.getSportsClass()!=null
                && orderClassId.equals(i.getSportsClass().getId()) && "Paid".equals(i.getPaymentStatus())
                && i.isReconciliationRequired() && !"Completed".equals(i.getResolutionStatus())))
            throw new BusinessException("PAYMENT_RECONCILIATION_REQUIRED: Resolve the previously received money before another order");
        invoiceRepository.findByMemberId(member.getId()).stream()
                .filter(i -> i.getSportsClass() != null && orderClassId.equals(i.getSportsClass().getId()))
                .filter(i -> "Pending".equals(i.getPaymentStatus())).forEach(i -> {
                    i.setPaymentStatus("Cancelled");
                });
        Invoice invoice = newInvoice(member, sportsClass.getTuitionFee(), method);
        invoice.setSportsClass(sportsClass);
        if (sportsClass.isNormal()) {
            var cutoff = com.sportscenter.sportclass.NormalClassPolicy.cutoff(sportsClass, normalSessions);
            if (invoice.getExpiresAt().isAfter(cutoff)) invoice.setExpiresAt(cutoff);
        }
        return orderResponse(invoiceRepository.save(invoice), clientIp);
    }

    @Transactional
    public void expireClassReservations(SportsClass c) {
        scheduleWriteLock.acquire(c.getCenterId());
        for (Invoice invoice : invoiceRepository.findBySportsClassIdAndPaymentStatusAndExpiresAtLessThanEqual(c.getId(),"Pending",LocalDateTime.now(clock)))
            expireInvoice(invoice.getInvoiceId());
    }

    public boolean hasExpiredClassOrder(Integer member,Integer classId) {
        return invoiceRepository.findByMemberId(member).stream().anyMatch(i -> i.getSportsClass()!=null && classId.equals(i.getSportsClass().getId()) && "Expired".equals(i.getPaymentStatus()));
    }

    private Invoice newInvoice(User member, BigDecimal amount, String method) {
        if (amount == null || amount.signum() <= 0) throw new BusinessException("INVALID_PRICE: A positive server catalog price is required");
        if (!"Cash".equalsIgnoreCase(method) && !"VNPay".equalsIgnoreCase(method)) {
            throw new BusinessException("Payment method must be Cash or VNPay");
        }
        Invoice invoice = new Invoice();
        invoice.setMember(member);
        invoice.setAmount(amount);
        invoice.setPaymentMethod("VNPay".equalsIgnoreCase(method) ? "VNPay" : "Cash");
        invoice.setPaymentStatus("Pending");
        LocalDateTime now = LocalDateTime.now(clock);
        invoice.setCreatedAt(now);
        invoice.setExpiresAt(now.plusMinutes(paymentProperties.getExpireMinutes()));
        long code = invoiceRepository.nextInvoiceCodeValue();
        invoice.setInvoiceCode(String.format("INV-%d-%06d", LocalDate.now(clock).getYear(), code));
        return invoice;
    }

    private void cancelPendingPackageOrders(Integer memberId, Integer packageId) {
        List<Invoice> previous = invoiceRepository.findByMemberId(memberId).stream()
                .filter(i -> i.getMembershipPackage() != null && packageId.equals(i.getMembershipPackage().getId()))
                .filter(i -> "Pending".equals(i.getPaymentStatus())).toList();
        for (Invoice invoice : previous) {
            invoice.setPaymentStatus("Cancelled");
            if (invoice.getSubscription() != null && "Pending".equals(invoice.getSubscription().getStatus())) {
                invoice.getSubscription().setStatus("Cancelled");
            }
        }
    }

    private InvoiceOrderResponse orderResponse(Invoice invoice, String clientIp) {
        String url = "VNPay".equals(invoice.getPaymentMethod()) ? issueVnpayLink(invoice, clientIp) : null;
        return new InvoiceOrderResponse(invoice.getInvoiceId(), invoice.getInvoiceCode(), invoice.getAmount(),
                invoice.getPaymentMethod(), invoice.getPaymentStatus(), invoice.getExpiresAt(), url);
    }

    @Transactional
    public InvoiceResponse confirmCash(Integer invoiceId, Authentication authentication) {
        return confirmCash(invoiceId,null,authentication);
    }
    @Transactional
    public InvoiceResponse confirmCash(Integer invoiceId,String note,Authentication authentication) {
        if(note!=null&&note.length()>2000)throw new BusinessException("CASH_NOTE_TOO_LONG: At most 2000 characters");
        User receptionist = currentUser(authentication);
        scheduleWriteLock.acquire(receptionist.getCenterId());
        Invoice invoice = lockInvoice(invoiceId);
        if (invoice.getSportsClass()!=null && invoice.getSportsClass().isNormal()
                && (!"Active".equalsIgnoreCase(receptionist.getStatus()) || !com.sportscenter.common.ActorPolicy.role(receptionist,"Receptionist")
                || authentication.getAuthorities().stream().noneMatch(a -> "MANAGE_INVOICES".equals(a.getAuthority()))))
            throw new AccessDeniedException("Active Receptionist with MANAGE_INVOICES required for Normal Cash");
        if (!java.util.Objects.equals(receptionist.getCenterId(), invoice.getMember().getCenterId())) throw new AccessDeniedException("Invoice belongs to another center");
        if (!"Cash".equals(invoice.getPaymentMethod())) throw new InvoiceConflictException("Only Cash invoices can be confirmed at the counter");
        if (!"Pending".equals(invoice.getPaymentStatus()) || expired(invoice)) throw new InvoiceConflictException("Invoice is no longer payable");
        String activationProblem=activationProblem(invoice);
        if(activationProblem!=null)throw new InvoiceConflictException("Invoice requires reconciliation before Cash collection: "+activationProblem);
        if(expired(invoice))throw new InvoiceConflictException("Invoice expired before Cash confirmation completed");
        invoice.setCashNote(note);
        markPaidAndActivate(invoice, receptionist, false);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Integer invoiceId, Authentication authentication) {
        User actor = currentUser(authentication);
        scheduleWriteLock.acquire(actor.getCenterId());
        Invoice invoice = lockInvoice(invoiceId);
        if (!java.util.Objects.equals(actor.getCenterId(), invoice.getMember().getCenterId())) throw new AccessDeniedException("Invoice belongs to another center");
        boolean member = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()));
        if (member && !invoice.getMember().getId().equals(actor.getId())) throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        if (!"Pending".equals(invoice.getPaymentStatus())) throw new InvoiceConflictException("Only Pending invoices can be cancelled");
        invoice.setPaymentStatus("Cancelled");
        cancelLinked(invoice);
        auditService.log(actor.getId(), "CANCEL_INVOICE", "invoices", invoiceId, "Invoice cancelled by user");
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public String payOnline(Integer invoiceId, Authentication authentication, String clientIp) {
        User actor = currentUser(authentication);
        scheduleWriteLock.acquire(actor.getCenterId());
        Invoice invoice = lockInvoice(invoiceId);
        if (!invoice.getMember().getId().equals(actor.getId())) throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        if (!"VNPay".equals(invoice.getPaymentMethod()) || !"Pending".equals(invoice.getPaymentStatus()) || expired(invoice)) {
            throw new InvoiceConflictException("Invoice is not payable online");
        }
        return issueVnpayLink(invoice, clientIp);
    }

    private String issueVnpayLink(Invoice invoice, String clientIp) {
        if (invoice.getSportsClass()!=null && invoice.getSportsClass().isNormal())
            com.sportscenter.sportclass.NormalClassPolicy.requireAdmission(invoice.getSportsClass(),normalSessions,clock);
        VnPayGateway.PaymentLink link = vnPayGateway.createPaymentLink(invoice, clientIp);
        if (!paymentAttemptRepository.existsByInvoiceInvoiceIdAndTmnCodeAndTxnRefAndTransactionDate(
                invoice.getInvoiceId(), link.tmnCode(), link.txnRef(), link.createDate())) {
            VnPayPaymentAttempt attempt = new VnPayPaymentAttempt();
            attempt.setInvoice(invoice); attempt.setTmnCode(link.tmnCode()); attempt.setTxnRef(link.txnRef());
            attempt.setTransactionDate(link.createDate()); attempt.setAmountMinor(link.amountMinor());
            attempt.setCreatedAt(LocalDateTime.now(clock));
            paymentAttemptRepository.save(attempt);
        }
        return link.url();
    }

    @Transactional
    public VnPayResult processVnpayResult(Map<String, String> params) {
        if (!vnPayGateway.isValid(params)) return new VnPayResult("97", "Invalid signature", null);
        Integer centerId = invoiceRepository.findCenterIdByCode(params.get("vnp_TxnRef")).orElse(null);
        if (centerId == null) return new VnPayResult("01", "Order not found", null);
        scheduleWriteLock.acquire(centerId);
        invoiceRepository.findSportsClassIdByInvoiceCode(params.get("vnp_TxnRef")).ifPresent(sportsClassRepository::findByIdForUpdate);
        Invoice invoice = invoiceRepository.findByInvoiceCodeForUpdate(params.get("vnp_TxnRef"))
                .orElse(null);
        if (invoice == null) return new VnPayResult("01", "Order not found", null);
        if (!"VNPay".equals(invoice.getPaymentMethod())) return new VnPayResult("99", "Invalid payment method", invoice);
        long callbackAmount;
        try { callbackAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "")); }
        catch (NumberFormatException ex) { return new VnPayResult("04", "Invalid amount", invoice); }
        if (invoice.getAmount().movePointRight(2).longValue() != callbackAmount) return new VnPayResult("04", "Invalid amount", invoice);
        if ("Paid".equals(invoice.getPaymentStatus())) return new VnPayResult("02", "Order already confirmed", invoice);

        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        if (success) {
            recordVnpaySuccess(invoice, params.get("vnp_TransactionNo"));
            return new VnPayResult("00", "Confirm Success", invoice);
        }
        if ("Pending".equals(invoice.getPaymentStatus()) || "Expired".equals(invoice.getPaymentStatus())) {
            if ("Pending".equals(invoice.getPaymentStatus())) invoice.setPaymentStatus("Failed");
            cancelLinked(invoice);
            auditService.log(null, "PAYMENT_FAILED", "invoices", invoice.getInvoiceId(), "VNPay response " + params.get("vnp_ResponseCode"));
        }
        return new VnPayResult("00", "Confirm Success", invoice);
    }

    private void recordVnpaySuccess(Invoice invoice, String transactionNo) {
        String problem = !"Pending".equals(invoice.getPaymentStatus()) || expired(invoice) ? null : activationProblem(invoice);
        boolean reconcile = !"Pending".equals(invoice.getPaymentStatus()) || expired(invoice) || problem != null;
        invoice.setGatewayTransactionRef(transactionNo);
        if (reconcile) {
            invoice.setPaymentStatus("Paid"); invoice.setReconciliationRequired(true);
            invoice.setPaymentDate(LocalDateTime.now(clock)); cancelLinked(invoice);
            normalRefunds.lateCancelledReceipt(invoice);
            auditService.log(null, "PAYMENT_RECONCILIATION_REQUIRED", "invoices", invoice.getInvoiceId(),
                    "Verified money received without an available entitlement; " + problem);
        } else markPaidAndActivate(invoice, null, false);
    }

    @Transactional
    public VnPayVerificationService.Claim claimVnpayVerification(Integer id, Authentication auth) {
        Invoice invoice = lockOwnVnpayInvoice(id, auth);
        if ("Paid".equals(invoice.getPaymentStatus())) return queryClaim(invoice, "ALREADY_PAID", "Hóa đơn đã được ghi nhận thanh toán.", 0);
        if ("Refunded".equals(invoice.getPaymentStatus())) return queryClaim(invoice, "NOT_APPLICABLE", "Hóa đơn đã hoàn tiền; cần theo dõi hồ sơ đối soát.", 0);
        if (!paymentProperties.isVnpayQueryEnabled()) return queryClaim(invoice, "DISABLED", "Kiểm tra VNPay chưa được bật trên backend.", 0);
        List<VnPayPaymentAttempt> attempts = paymentAttemptRepository.findByInvoiceInvoiceIdOrderByIdDesc(id);
        if (attempts.isEmpty()) return queryClaim(invoice, "ATTEMPT_MISSING", "Đơn cũ thiếu dữ liệu tạo link VNPay; cần đối soát giao dịch gốc.", 0);
        LocalDateTime now = LocalDateTime.now(clock);
        var last = attempts.stream().map(VnPayPaymentAttempt::getLastQueryAt).filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo);
        if (last.isPresent() && last.get().plusSeconds(30).isAfter(now)) {
            int seconds = Math.max(1, (int) java.time.Duration.between(now, last.get().plusSeconds(30)).toSeconds() + 1);
            return queryClaim(invoice, "RETRY_LATER", "Vui lòng chờ trước khi kiểm tra lại VNPay.", seconds);
        }
        // Rotate through old requests as well: reissuing a URL must not hide a paid older request.
        attempts.sort(java.util.Comparator.comparing(VnPayPaymentAttempt::getLastQueryAt,
                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        var selected = attempts.stream().limit(3).toList();
        selected.forEach(attempt -> attempt.setLastQueryAt(now));
        paymentAttemptRepository.saveAll(selected);
        return new VnPayVerificationService.Claim(null, selected.stream().map(VnPayPaymentAttempt::snapshot).toList());
    }

    @Transactional
    public VnPayVerificationResponse settleVnpayQuery(Integer id, Authentication auth, Long attemptId, VerifiedVnPayQuery result) {
        Invoice invoice = lockOwnVnpayInvoice(id, auth);
        if ("Paid".equals(invoice.getPaymentStatus())) return queryResponse(invoice, "ALREADY_PAID", "Hóa đơn đã được ghi nhận thanh toán.", 0);
        if ("Refunded".equals(invoice.getPaymentStatus())) return queryResponse(invoice, "NOT_APPLICABLE", "Hóa đơn đã hoàn tiền.", 0);
        VnPayPaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException("PAYMENT_ATTEMPT_NOT_FOUND"));
        if (!attempt.getInvoice().getInvoiceId().equals(id) || !result.successful()
                || !attempt.getTmnCode().equals(result.tmnCode()) || !paymentProperties.getVnpayTmnCode().equals(result.tmnCode())
                || !invoice.getInvoiceCode().equals(result.txnRef()) || !attempt.getTxnRef().equals(result.txnRef())
                || attempt.getAmountMinor() != result.amountMinor() || invoice.getAmount().movePointRight(2).longValueExact() != result.amountMinor()
                || result.transactionNo() == null || !result.transactionNo().matches("[0-9]{1,15}"))
            throw new BusinessException("PAYMENT_EVIDENCE_MISMATCH");
        if (invoiceRepository.existsByGatewayTransactionRefAndInvoiceIdNot(result.transactionNo(), id))
            throw new InvoiceConflictException("PAYMENT_EVIDENCE_CONFLICT: Transaction already recorded");
        if (!paymentProperties.isVnpayQuerySettlementEnabled())
            return queryResponse(invoice, "VERIFIED_ONLY", "VNPay đã xác nhận; chế độ kiểm chứng chưa cập nhật hóa đơn.", 30);
        recordVnpaySuccess(invoice, result.transactionNo());
        auditService.log(invoice.getMember().getId(), "VERIFY_VNPAY_QUERY", "invoices", id,
                "Verified QueryDR response " + result.responseId());
        return queryResponse(invoice, "VERIFIED_PAID", "Đã xác minh thanh toán với VNPay.", 0);
    }

    @Transactional(readOnly = true)
    public VnPayVerificationResponse vnpayVerificationState(Integer id, Authentication auth, String status, String message, int retry) {
        User actor = queryActor(auth);
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        checkOwnInvoice(invoice, actor);
        if ("Paid".equals(invoice.getPaymentStatus())) return queryResponse(invoice, "ALREADY_PAID", "Hóa đơn đã được ghi nhận thanh toán.", 0);
        return queryResponse(invoice, status, message, retry);
    }

    private Invoice lockOwnVnpayInvoice(Integer id, Authentication auth) {
        User actor = queryActor(auth);
        Integer center = invoiceRepository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!java.util.Objects.equals(center, actor.getCenterId())) throw new ResourceNotFoundException("Invoice not found");
        scheduleWriteLock.acquire(center);
        Invoice invoice = lockInvoice(id);
        checkOwnInvoice(invoice, actor);
        return invoice;
    }

    private User queryActor(Authentication auth) {
        User actor = currentUser(auth);
        if (!"Active".equalsIgnoreCase(actor.getStatus()) || actor.getCenterId() == null
                || actor.getRole() == null || !"Member".equalsIgnoreCase(actor.getRole().getName())
                || memberRepository.findById(actor.getId()).isEmpty())
            throw new AccessDeniedException("Active Member account is required");
        return actor;
    }

    private void checkOwnInvoice(Invoice invoice, User actor) {
        if (!invoice.getMember().getId().equals(actor.getId()) || !java.util.Objects.equals(invoice.getMember().getCenterId(), actor.getCenterId()))
            throw new ResourceNotFoundException("Invoice not found");
        if (!"VNPay".equals(invoice.getPaymentMethod())) throw new BusinessException("VNPAY_INVOICE_REQUIRED");
    }

    private VnPayVerificationService.Claim queryClaim(Invoice invoice, String status, String message, int retry) {
        return new VnPayVerificationService.Claim(queryResponse(invoice, status, message, retry), List.of());
    }

    private VnPayVerificationResponse queryResponse(Invoice invoice, String status, String message, int retry) {
        return new VnPayVerificationResponse(status, message, retry, InvoiceResponse.from(invoice));
    }

    @Transactional(readOnly = true)
    public List<Integer> findExpiredPendingInvoiceIds() {
        return invoiceRepository.findByPaymentStatusAndExpiresAtBefore("Pending", LocalDateTime.now(clock)).stream()
                .map(Invoice::getInvoiceId).toList();
    }

    @Transactional
    public boolean expireInvoice(Integer invoiceId) {
        Integer centerId = invoiceRepository.findCenterIdById(invoiceId).orElse(null);
        if (centerId == null) return false;
        scheduleWriteLock.acquire(centerId);
        invoiceRepository.findSportsClassIdByInvoiceId(invoiceId).ifPresent(sportsClassRepository::findByIdForUpdate);
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElse(null);
        if (invoice == null || !"Pending".equals(invoice.getPaymentStatus()) || !expired(invoice)) return false;
        invoice.setPaymentStatus("Expired");
        expireLinked(invoice);
        auditService.log(null, "EXPIRE_INVOICE", "invoices", invoice.getInvoiceId(), "Pending invoice expired");
        eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.PAYMENT_EXPIRED,
                List.of(invoice.getMember().getId()),
                NotificationTemplates.paymentExpired(invoice.getInvoiceCode()),
                "INVOICE", invoice.getInvoiceId(), "PAYMENT_EXPIRED:" + invoice.getInvoiceId()));
        return true;
    }

    private void markPaidAndActivate(Invoice invoice, User receptionist, boolean paidAfterExpiry) {
        if (!"Pending".equals(invoice.getPaymentStatus()) && !(paidAfterExpiry && "Expired".equals(invoice.getPaymentStatus()))) {
            throw new InvoiceConflictException("Invoice is no longer payable");
        }
        if (receptionist != null) invoice.setReceptionist(receptionist);
        invoice.setPaymentStatus("Paid");
        invoice.setPaymentDate(LocalDateTime.now(clock));
        if (invoice.getSubscription() != null) {
            invoice.getSubscription().setPurchaseAmount(invoice.getAmount());
            activateSubscription(invoice.getSubscription(),invoice.getPaymentDate());
        }
        if (invoice.getSportsClass() != null) activateClassEnrollment(invoice);
        eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.PAYMENT_SUCCESS,
                List.of(invoice.getMember().getId()),
                NotificationTemplates.paymentSuccess(invoice.getInvoiceCode()),
                "INVOICE", invoice.getInvoiceId(), "PAYMENT_SUCCESS:" + invoice.getInvoiceId()));
        if (invoice.getSportsClass() != null && !invoice.getSportsClass().isAdvanced()) {
            eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.ENROLLMENT_CONFIRMED,
                    List.of(invoice.getMember().getId()),
                    NotificationTemplates.enrollmentConfirmed(invoice.getSportsClass().getName()),
                    "CLASS", invoice.getSportsClass().getId(), "ENROLLMENT_CONFIRMED:" + invoice.getInvoiceId()));
        }
        auditService.log(receptionist != null ? receptionist.getId() : null,
                paidAfterExpiry ? "PAID_AFTER_EXPIRY" : "PROCESS_PAYMENT", "invoices", invoice.getInvoiceId(),
                "Invoice " + invoice.getInvoiceCode() + " paid by " + invoice.getPaymentMethod());
    }

    private void activateSubscription(MemberPackage subscription,LocalDateTime paidAt) {
        MembershipPackage pkg = subscription.getMembershipPackage();
        LocalDate now = paidAt.toLocalDate();
        if (subscription.isAdvanced()) {
            if (subscription.getSubject() == null || subscription.getPrimaryCoach() == null)
                throw new BusinessException("ENTITLEMENT_RECONCILIATION_REQUIRED: Legacy Advanced order requires subject and Coach evidence");
            int days = 45;
            int credits = 12;
            subscription.setActivatedAt(paidAt);
            entitlementTimePolicy.recalculate(subscription);
            subscription.setStartDate(now);
            subscription.setRemainingCredit(credits);
            subscription.setValidityDays(days);
            subscription.setStatus("Active");
        } else {
            LocalDate start = now;
            MemberPackage latestActive = memberPackageRepository.findByMemberId(subscription.getMember().getId()).stream()
                    .filter(mp -> mp != subscription && "Active".equalsIgnoreCase(mp.getStatus())
                            && (mp.getMembershipPackage() == null || !mp.getMembershipPackage().isAdvanced())
                            && mp.getEndDate() != null)
                    .max(java.util.Comparator.comparing(MemberPackage::getEndDate)).orElse(null);
            if (latestActive != null && !latestActive.getEndDate().isBefore(start)) start = latestActive.getEndDate().plusDays(1);
            int days = (pkg != null && pkg.getDurationDays() != null) ? pkg.getDurationDays() : 30;
            subscription.setStartDate(start);
            subscription.setEndDate(start.plusDays(days));
            subscription.setRemainingCredit(0);
            subscription.setStatus("Active");
        }
    }

    private void activateClassEnrollment(Invoice invoice) {
        SportsClass lockedClass = sportsClassRepository.findByIdForUpdate(invoice.getSportsClass().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        long occupied = enrollmentRepository.countBySportsClassIdAndStatusIn(lockedClass.getId(), List.of("Registered", "Pending"));
        if (lockedClass.getMaxCapacity() != null && occupied > lockedClass.getMaxCapacity()) {
            throw new BusinessException("Class capacity is full; payment requires manual reconciliation");
        }
        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(invoice.getMember().getId(), lockedClass.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class enrollment not found"));
        enrollment.setStatus("Registered");
    }
    private String activationProblem(Invoice invoice){
        if(!"Active".equalsIgnoreCase(invoice.getMember().getStatus()))return "Member account is inactive";
        var profile=memberRepository.findById(invoice.getMember().getId());
        if(profile.isPresent()&&profile.get().getBookingBlockedUntil()!=null&&profile.get().getBookingBlockedUntil().isAfter(LocalDateTime.now(clock)))return "Member booking is blocked";
        MemberPackage sub=invoice.getSubscription();
        if(sub!=null){
            if(!"Pending".equals(sub.getStatus()))return "Subscription is no longer Pending";
            if(sub.isAdvanced()){
                if(sub.getSubject()==null||sub.getPrimaryCoach()==null)return "Legacy subject or Primary Coach evidence is missing";
                User coach=sub.getPrimaryCoach();
                if(!"Active".equalsIgnoreCase(sub.getSubject().getStatus())||!"Active".equalsIgnoreCase(coach.getStatus())||coach.getRole()==null||!"Coach".equalsIgnoreCase(coach.getRole().getName())||!java.util.Objects.equals(coach.getCenterId(),invoice.getMember().getCenterId()))return "Subject or Primary Coach is unavailable";
                return coachCapacityService.activationProblem(coach);
            }
            if(sub.getMembershipPackage()==null||!"Active".equalsIgnoreCase(sub.getMembershipPackage().getStatus()))return "Membership package is unavailable";
            return null;
        }
        if(invoice.getSportsClass()!=null){
            SportsClass c=invoice.getSportsClass();
            if(c.isNormal()) {
                if(c.getSubject()==null || !"Active".equalsIgnoreCase(c.getSubject().getStatus())) return "Subject is no longer active";
                try {
                    com.sportscenter.sportclass.NormalClassPolicy.requireAdmission(c, normalSessions, clock);
                    com.sportscenter.session.NormalSchedulePolicy.requireReady(c,normalSessions,clock);
                }
                catch (BusinessException closed) { return closed.getMessage(); }
            }
            if(!"Open".equals(c.getStatus())&&!"Ongoing".equals(c.getStatus()))return "Class is no longer open";
            var enrollment=enrollmentRepository.findByMemberIdAndSportsClassId(invoice.getMember().getId(),c.getId());
            if(enrollment.isEmpty()||!"Pending".equals(enrollment.get().getStatus()))return "Enrollment is no longer Pending";
            if(c.getMaxCapacity()!=null&&enrollmentRepository.countBySportsClassIdAndStatusIn(c.getId(),List.of("Registered","Pending"))>c.getMaxCapacity())return "Class capacity changed";
            return null;
        }
        return "Entitlement link is missing";
    }

    private void cancelLinked(Invoice invoice) {
        if (invoice.getSubscription() != null && "Pending".equals(invoice.getSubscription().getStatus())) invoice.getSubscription().setStatus("Cancelled");
        if (invoice.getSportsClass() != null && !hasNewerPendingClassOrder(invoice)) enrollmentRepository.findByMemberIdAndSportsClassId(invoice.getMember().getId(), invoice.getSportsClass().getId())
                .filter(e -> "Pending".equals(e.getStatus())).ifPresent(e -> e.setStatus("Cancelled"));
    }

    private void expireLinked(Invoice invoice) {
        if (invoice.getSubscription() != null && "Pending".equals(invoice.getSubscription().getStatus())) {
            invoice.getSubscription().setStatus("Cancelled");
        }
        if (invoice.getSportsClass() != null && !hasNewerPendingClassOrder(invoice)) {
            enrollmentRepository.findByMemberIdAndSportsClassId(invoice.getMember().getId(), invoice.getSportsClass().getId())
                    .filter(e -> "Pending".equals(e.getStatus())).ifPresent(e -> e.setStatus("Cancelled"));
        }
    }

    @Transactional
    public void cancelPendingClassPayment(Integer memberId, Integer classId) {
        invoiceRepository.findByMemberId(memberId).stream()
                .filter(i -> i.getSportsClass() != null && classId.equals(i.getSportsClass().getId()))
                .filter(i -> "Pending".equals(i.getPaymentStatus()) || "Expired".equals(i.getPaymentStatus()))
                .map(Invoice::getInvoiceId).toList().forEach(id -> {
                    Invoice invoice = invoiceRepository.findByIdForUpdate(id).orElse(null);
                    if (invoice != null && ("Pending".equals(invoice.getPaymentStatus()) || "Expired".equals(invoice.getPaymentStatus()))) {
                        invoice.setPaymentStatus("Cancelled");
                        cancelLinked(invoice);
                    }
                });
    }

    private Invoice lockInvoice(Integer id) {
        invoiceRepository.findSportsClassIdByInvoiceId(id).ifPresent(sportsClassRepository::findByIdForUpdate);
        return invoiceRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }
    private boolean hasNewerPendingClassOrder(Invoice invoice) {
        return invoiceRepository.existsByMemberIdAndSportsClassIdAndPaymentStatusAndInvoiceIdNot(
                invoice.getMember().getId(), invoice.getSportsClass().getId(), "Pending", invoice.getInvoiceId());
    }
    @Transactional(readOnly = true)
    public void requirePaidSubscription(Integer subscriptionId) {
        if (!invoiceRepository.existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(subscriptionId, "Paid")) {
            throw new BusinessException("A subscription can only be activated after its invoice is Paid");
        }
    }

    @Transactional
    public void cancelPendingSubscriptionPayment(Integer subscriptionId) {
        for (Invoice candidate : invoiceRepository.findBySubscriptionSubscriptionId(subscriptionId)) {
            Invoice invoice = lockInvoice(candidate.getInvoiceId());
            if ("Pending".equals(invoice.getPaymentStatus())) {
                invoice.setPaymentStatus("Cancelled");
                cancelLinked(invoice);
            }
        }
    }
    private boolean expired(Invoice invoice) { return invoice.getExpiresAt() == null || !invoice.getExpiresAt().isAfter(LocalDateTime.now(clock)); }
    private User currentUser(Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authenticated user is required");
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
    }
    private Integer resolveMemberScope(Authentication authentication, Integer requestedMemberId) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()))) return requestedMemberId;
        Integer actorId = currentUser(authentication).getId();
        if (requestedMemberId != null && !requestedMemberId.equals(actorId)) throw new AccessDeniedException("Members may only view their own invoices");
        return actorId;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    public record VnPayResult(String code, String message, Invoice invoice) { }
}
