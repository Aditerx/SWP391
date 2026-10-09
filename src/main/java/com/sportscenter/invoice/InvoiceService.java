package com.sportscenter.invoice;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(
            Integer memberId,
            Integer packageId,
            Integer receptionistId,
            String status,
            String method,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Authentication authentication
    ) {
        Integer scopedMemberId = resolveMemberScope(authentication, memberId);
        List<Invoice> list = scopedMemberId == null
                ? invoiceRepository.findAllWithDetails()
                : invoiceRepository.findByMemberId(scopedMemberId);
        return list.stream()
                .filter(i -> scopedMemberId == null || (i.getMember() != null && scopedMemberId.equals(i.getMember().getId())))
                .filter(i -> packageId == null || (i.getMembershipPackage() != null && packageId.equals(i.getMembershipPackage().getId())))
                .filter(i -> receptionistId == null || (i.getReceptionist() != null && receptionistId.equals(i.getReceptionist().getId())))
                .filter(i -> status == null || status.isBlank() || (i.getPaymentStatus() != null && i.getPaymentStatus().equalsIgnoreCase(status)))
                .filter(i -> method == null || method.isBlank() || (i.getPaymentMethod() != null && i.getPaymentMethod().equalsIgnoreCase(method)))
                .filter(i -> startDate == null || (i.getPaymentDate() != null && !i.getPaymentDate().isBefore(startDate)))
                .filter(i -> endDate == null || (i.getPaymentDate() != null && !i.getPaymentDate().isAfter(endDate)))
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Integer id, Authentication authentication) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        Integer scopedMemberId = resolveMemberScope(authentication, null);
        if (scopedMemberId != null && (invoice.getMember() == null || !scopedMemberId.equals(invoice.getMember().getId()))) {
            throw new AccessDeniedException("Members may only view their own invoices");
        }
        return InvoiceResponse.from(invoice);
    }

    private Integer resolveMemberScope(Authentication authentication, Integer requestedMemberId) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()))) {
            return requestedMemberId;
        }

        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated member account was not found"));
        if (requestedMemberId != null && !requestedMemberId.equals(currentUser.getId())) {
            throw new AccessDeniedException("Members may only view their own invoices");
        }
        return currentUser.getId();
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));

        MembershipPackage pkg = null;
        if (request.packageId() != null) {
            pkg = membershipPackageRepository.findById(request.packageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + request.packageId()));
        }

        User receptionist = null;
        if (request.receptionistId() != null) {
            receptionist = userRepository.findById(request.receptionistId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receptionist not found: " + request.receptionistId()));
        }

        String paymentStatus = normalizeStatus(request.paymentStatus());
        String method = normalizeMethod(request.paymentMethod());

        Invoice invoice = new Invoice();
        invoice.setMember(member);
        invoice.setMembershipPackage(pkg);
        invoice.setReceptionist(receptionist);
        invoice.setAmount(request.amount());
        invoice.setPaymentMethod(method);
        invoice.setPaymentStatus(paymentStatus);

        if ("Paid".equals(paymentStatus)) {
            invoice.setPaymentDate(LocalDateTime.now());
            invoice.setGatewayTransactionRef(
                    request.gatewayTransactionRef() != null && !request.gatewayTransactionRef().isBlank()
                            ? request.gatewayTransactionRef()
                            : "TXN-" + System.currentTimeMillis() % 1000000
            );
        } else {
            invoice.setGatewayTransactionRef(request.gatewayTransactionRef());
        }

        Invoice saved = invoiceRepository.save(invoice);

        String detail = "Created invoice #" + saved.getInvoiceId() + " for member " + member.getFullName() +
                ", amount: " + saved.getAmount() + " [" + saved.getPaymentStatus() + "]";
        auditService.log(receptionist != null ? receptionist.getId() : null, "PROCESS_PAYMENT", "invoices", saved.getInvoiceId(), detail);

        return InvoiceResponse.from(saved);
    }

    @Transactional
    public InvoiceResponse payInvoice(Integer invoiceId, InvoicePaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if ("Paid".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new BusinessException("Invoice #" + invoiceId + " is already paid");
        }

        String method = normalizeMethod(request.paymentMethod());
        invoice.setPaymentMethod(method);
        invoice.setPaymentStatus("Paid");
        invoice.setPaymentDate(LocalDateTime.now());

        String ref = request.gatewayTransactionRef();
        if (ref == null || ref.isBlank()) {
            ref = "TXN-" + System.currentTimeMillis() % 1000000;
        }
        invoice.setGatewayTransactionRef(ref);

        if (request.receptionistId() != null) {
            User receptionist = userRepository.findById(request.receptionistId()).orElse(null);
            if (receptionist != null) {
                invoice.setReceptionist(receptionist);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);

        String detail = "Processed payment for invoice #" + invoiceId + " (" + saved.getAmount() + " via " + method + ", Ref: " + ref + ")";
        auditService.log(invoice.getReceptionist() != null ? invoice.getReceptionist().getId() : null,
                "PROCESS_PAYMENT", "invoices", saved.getInvoiceId(), detail);

        return InvoiceResponse.from(saved);
    }

    @Transactional
    public InvoiceResponse updateStatus(Integer invoiceId, String status) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        String oldStatus = invoice.getPaymentStatus();
        String newStatus = normalizeStatus(status);
        invoice.setPaymentStatus(newStatus);

        if ("Paid".equalsIgnoreCase(newStatus) && invoice.getPaymentDate() == null) {
            invoice.setPaymentDate(LocalDateTime.now());
            if (invoice.getGatewayTransactionRef() == null) {
                invoice.setGatewayTransactionRef("TXN-" + System.currentTimeMillis() % 1000000);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);

        auditService.log(null, "UPDATE_INVOICE_STATUS", "invoices", saved.getInvoiceId(),
                "Status changed: " + oldStatus + " -> " + newStatus);

        return InvoiceResponse.from(saved);
    }

    @Transactional
    public Invoice createInvoiceForSubscription(User member, MembershipPackage pkg, User receptionist, BigDecimal amount, String method) {
        Invoice invoice = new Invoice();
        invoice.setMember(member);
        invoice.setMembershipPackage(pkg);
        invoice.setReceptionist(receptionist);
        invoice.setAmount(amount != null ? amount : (pkg != null ? pkg.getPrice() : BigDecimal.ZERO));

        String normMethod = normalizeMethod(method);
        invoice.setPaymentMethod(normMethod);

        if (normMethod != null && !"Pending".equalsIgnoreCase(normMethod)) {
            invoice.setPaymentStatus("Paid");
            invoice.setPaymentDate(LocalDateTime.now());
            invoice.setGatewayTransactionRef("TXN-" + System.currentTimeMillis() % 1000000);
        } else {
            invoice.setPaymentStatus("Paid");
            invoice.setPaymentDate(LocalDateTime.now());
            invoice.setPaymentMethod("Cash");
        }

        return invoiceRepository.save(invoice);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "Pending";
        if ("Paid".equalsIgnoreCase(status) || "successful".equalsIgnoreCase(status)) return "Paid";
        if ("Failed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) return "Failed";
        if ("Refunded".equalsIgnoreCase(status)) return "Refunded";
        return "Pending";
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) return "Cash";
        if ("BankTransfer".equalsIgnoreCase(method) || "bank_transfer".equalsIgnoreCase(method)) return "BankTransfer";
        if ("CreditCard".equalsIgnoreCase(method) || "card_pos".equalsIgnoreCase(method)) return "CreditCard";
        if ("EWallet".equalsIgnoreCase(method) || "ewallet".equalsIgnoreCase(method)) return "EWallet";
        if ("Cash".equalsIgnoreCase(method) || "cash".equalsIgnoreCase(method)) return "Cash";
        return "Cash";
    }
}
