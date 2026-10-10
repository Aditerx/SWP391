package com.sportscenter.invoice;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceService {
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

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(Integer memberId, Integer packageId, Integer receptionistId,
            String status, String method, String keyword, LocalDateTime startDate, LocalDateTime endDate,
            Authentication authentication) {
        Integer scopedMemberId = resolveMemberScope(authentication, memberId);
        List<Invoice> list = scopedMemberId != null
                ? invoiceRepository.findByMemberId(scopedMemberId)
                : invoiceRepository.searchInvoices(null, packageId, receptionistId, blankToNull(status), blankToNull(method), blankToNull(keyword), startDate, endDate);
        return list.stream().map(InvoiceResponse::from).toList();
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
        if (scopedMemberId != null && !scopedMemberId.equals(invoice.getMember().getId())) {
            throw new ResourceNotFoundException("Invoice not found: " + id);
        }
        return InvoiceResponse.from(invoice);
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
        invoiceRepository.findByMemberId(member.getId()).stream()
                .filter(i -> i.getSportsClass() != null && sportsClass.getId().equals(i.getSportsClass().getId()))
                .filter(i -> "Pending".equals(i.getPaymentStatus())).forEach(i -> {
                    i.setPaymentStatus("Cancelled");
                });
        Invoice invoice = newInvoice(member, sportsClass.getTuitionFee(), method);
        invoice.setSportsClass(sportsClass);
        return orderResponse(invoiceRepository.save(invoice), clientIp);
    }

    private Invoice newInvoice(User member, BigDecimal amount, String method) {
        if (!"Cash".equalsIgnoreCase(method) && !"VNPay".equalsIgnoreCase(method)) {
            throw new BusinessException("Payment method must be Cash or VNPay");
        }
        Invoice invoice = new Invoice();
        invoice.setMember(member);
        invoice.setAmount(amount);
        invoice.setPaymentMethod("VNPay".equalsIgnoreCase(method) ? "VNPay" : "Cash");
        invoice.setPaymentStatus("Pending");
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        invoice.setCreatedAt(now);
        invoice.setExpiresAt(now.plusMinutes(paymentProperties.getExpireMinutes()));
        long code = invoiceRepository.nextInvoiceCodeValue();
        invoice.setInvoiceCode(String.format("INV-%d-%06d", LocalDate.now(BUSINESS_ZONE).getYear(), code));
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
        String url = "VNPay".equals(invoice.getPaymentMethod()) ? vnPayGateway.createPaymentUrl(invoice, clientIp) : null;
        return new InvoiceOrderResponse(invoice.getInvoiceId(), invoice.getInvoiceCode(), invoice.getAmount(),
                invoice.getPaymentMethod(), invoice.getPaymentStatus(), invoice.getExpiresAt(), url);
    }

    @Transactional
    public InvoiceResponse confirmCash(Integer invoiceId, Authentication authentication) {
        User receptionist = currentUser(authentication);
        Invoice invoice = lockInvoice(invoiceId);
        if (!"Cash".equals(invoice.getPaymentMethod())) throw new InvoiceConflictException("Only Cash invoices can be confirmed at the counter");
        if (!"Pending".equals(invoice.getPaymentStatus()) || expired(invoice)) throw new InvoiceConflictException("Invoice is no longer payable");
        markPaidAndActivate(invoice, receptionist, false);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Integer invoiceId, Authentication authentication) {
        User actor = currentUser(authentication);
        Invoice invoice = lockInvoice(invoiceId);
        boolean member = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()));
        if (member && !invoice.getMember().getId().equals(actor.getId())) throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        if (!"Pending".equals(invoice.getPaymentStatus())) throw new InvoiceConflictException("Only Pending invoices can be cancelled");
        invoice.setPaymentStatus("Cancelled");
        cancelLinked(invoice);
        auditService.log(actor.getId(), "CANCEL_INVOICE", "invoices", invoiceId, "Invoice cancelled by user");
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public String payOnline(Integer invoiceId, Authentication authentication, String clientIp) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (!invoice.getMember().getId().equals(currentUser(authentication).getId())) throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        if (!"VNPay".equals(invoice.getPaymentMethod()) || !"Pending".equals(invoice.getPaymentStatus()) || expired(invoice)) {
            throw new InvoiceConflictException("Invoice is not payable online");
        }
        return vnPayGateway.createPaymentUrl(invoice, clientIp);
    }

    @Transactional
    public VnPayResult processVnpayResult(Map<String, String> params) {
        if (!vnPayGateway.isValid(params)) return new VnPayResult("97", "Invalid signature", null);
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
            if (!"Pending".equals(invoice.getPaymentStatus()) && !"Expired".equals(invoice.getPaymentStatus())) {
                return new VnPayResult("99", "Order is no longer payable", invoice);
            }
            boolean paidAfterExpiry = "Expired".equals(invoice.getPaymentStatus()) || expired(invoice);
            invoice.setGatewayTransactionRef(params.get("vnp_TransactionNo"));
            markPaidAndActivate(invoice, null, paidAfterExpiry);
            return new VnPayResult("00", "Confirm Success", invoice);
        }
        if ("Pending".equals(invoice.getPaymentStatus()) || "Expired".equals(invoice.getPaymentStatus())) {
            if ("Pending".equals(invoice.getPaymentStatus())) invoice.setPaymentStatus("Failed");
            cancelLinked(invoice);
            auditService.log(null, "PAYMENT_FAILED", "invoices", invoice.getInvoiceId(), "VNPay response " + params.get("vnp_ResponseCode"));
        }
        return new VnPayResult("00", "Confirm Success", invoice);
    }

    @Transactional(readOnly = true)
    public List<Integer> findExpiredPendingInvoiceIds() {
        return invoiceRepository.findByPaymentStatusAndExpiresAtBefore("Pending", LocalDateTime.now(BUSINESS_ZONE)).stream()
                .map(Invoice::getInvoiceId).toList();
    }

    @Transactional
    public boolean expireInvoice(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElse(null);
        if (invoice == null || !"Pending".equals(invoice.getPaymentStatus()) || !expired(invoice)) return false;
        invoice.setPaymentStatus("Expired");
        expireLinked(invoice);
        auditService.log(null, "EXPIRE_INVOICE", "invoices", invoice.getInvoiceId(), "Pending invoice expired");
        return true;
    }

    private void markPaidAndActivate(Invoice invoice, User receptionist, boolean paidAfterExpiry) {
        if (!"Pending".equals(invoice.getPaymentStatus()) && !(paidAfterExpiry && "Expired".equals(invoice.getPaymentStatus()))) {
            throw new InvoiceConflictException("Invoice is no longer payable");
        }
        if (receptionist != null) invoice.setReceptionist(receptionist);
        invoice.setPaymentStatus("Paid");
        invoice.setPaymentDate(LocalDateTime.now(BUSINESS_ZONE));
        if (invoice.getSubscription() != null) activateSubscription(invoice.getSubscription());
        if (invoice.getSportsClass() != null) activateClassEnrollment(invoice);
        auditService.log(receptionist != null ? receptionist.getId() : null,
                paidAfterExpiry ? "PAID_AFTER_EXPIRY" : "PROCESS_PAYMENT", "invoices", invoice.getInvoiceId(),
                "Invoice " + invoice.getInvoiceCode() + " paid by " + invoice.getPaymentMethod());
    }

    private void activateSubscription(MemberPackage subscription) {
        LocalDate start = LocalDate.now(BUSINESS_ZONE);
        MemberPackage latestActive = memberPackageRepository.findByMemberId(subscription.getMember().getId()).stream()
                .filter(mp -> mp != subscription && "Active".equalsIgnoreCase(mp.getStatus()) && mp.getEndDate() != null)
                .max(java.util.Comparator.comparing(MemberPackage::getEndDate)).orElse(null);
        if (latestActive != null && !latestActive.getEndDate().isBefore(start)) start = latestActive.getEndDate().plusDays(1);
        int days = subscription.getMembershipPackage().getDurationDays() == null ? 30 : subscription.getMembershipPackage().getDurationDays();
        subscription.setStartDate(start);
        subscription.setEndDate(start.plusDays(days));
        subscription.setStatus("Active");
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

    private void cancelLinked(Invoice invoice) {
        if (invoice.getSubscription() != null && "Pending".equals(invoice.getSubscription().getStatus())) invoice.getSubscription().setStatus("Cancelled");
        if (invoice.getSportsClass() != null) enrollmentRepository.findByMemberIdAndSportsClassId(invoice.getMember().getId(), invoice.getSportsClass().getId())
                .filter(e -> "Pending".equals(e.getStatus())).ifPresent(e -> e.setStatus("Cancelled"));
    }

    private void expireLinked(Invoice invoice) {
        if (invoice.getSubscription() != null && "Pending".equals(invoice.getSubscription().getStatus())) {
            invoice.getSubscription().setStatus("Cancelled");
        }
        // Keep a VNPay class seat reserved until its signed callback reports success or failure.
        if (invoice.getSportsClass() != null && "Cash".equals(invoice.getPaymentMethod())) {
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
        return invoiceRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }
    private boolean expired(Invoice invoice) { return invoice.getExpiresAt() != null && !invoice.getExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE)); }
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
