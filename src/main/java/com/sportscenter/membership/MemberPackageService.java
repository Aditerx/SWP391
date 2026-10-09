package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.invoice.InvoiceService;
import com.sportscenter.invoice.InvoiceOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPackageService {
    private final MemberPackageRepository memberPackageRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public List<MemberPackageResponse> findAll() {
        return memberPackageRepository.findAllWithDetails().stream()
                .map(MemberPackageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPackageResponse> findByMemberId(Integer memberId, Authentication authentication) {
        User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
        boolean memberRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()))
                || (actor.getRole() != null && "Member".equalsIgnoreCase(actor.getRole().getName()));
        if (memberRole) {
            if (!actor.getId().equals(memberId)) {
                throw new AccessDeniedException("Members may only view their own subscriptions");
            }
            memberId = actor.getId();
        }
        return memberPackageRepository.findByMemberId(memberId).stream()
                .map(MemberPackageResponse::from)
                .toList();
    }

    @Transactional
    public SubscriptionOrderResponse subscribe(Integer memberId, MemberPackageRequest request) {
        return subscribe(memberId, request, "127.0.0.1");
    }

    @Transactional
    public SubscriptionOrderResponse subscribe(Integer memberId, MemberPackageRequest request, String clientIp) {
        User memberUser = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + memberId));

        MembershipPackage pkg = membershipPackageRepository.findById(request.packageId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found: " + request.packageId()));

        if (!"Active".equalsIgnoreCase(pkg.getStatus())) throw new BusinessException("Cannot subscribe to an Inactive membership package");
        String method = request.paymentMethod() == null || request.paymentMethod().isBlank() ? "Cash" : request.paymentMethod();
        LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int days = pkg.getDurationDays() != null ? pkg.getDurationDays() : 30;

        MemberPackage mp = new MemberPackage();
        mp.setMember(memberUser);
        mp.setMembershipPackage(pkg);
        mp.setStartDate(startDate);
        mp.setEndDate(startDate.plusDays(days));
        mp.setStatus("Pending");
        MemberPackage saved = memberPackageRepository.save(mp);
        InvoiceOrderResponse order = invoiceService.createPackageOrder(memberUser, pkg, method, saved, clientIp);
        auditService.log(null, "SUBSCRIBE_PACKAGE", "MEMBER_PACKAGE", saved.getSubscriptionId(),
                "Created pending subscription and invoice " + order.invoiceCode());
        return new SubscriptionOrderResponse(saved.getSubscriptionId(), order.invoiceId(), order.invoiceCode(),
                order.amount(), order.paymentMethod(), order.status(), order.expiresAt(), order.paymentUrl());
    }

    @Transactional
    public SubscriptionOrderResponse subscribeMe(MemberPackageRequest request, Authentication authentication) {
        return subscribeMe(request, authentication, "127.0.0.1");
    }

    @Transactional
    public SubscriptionOrderResponse subscribeMe(MemberPackageRequest request, Authentication authentication, String clientIp) {
        User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated member account was not found"));
        return subscribe(actor.getId(), request, clientIp);
    }

    @Transactional
    public MemberPackageResponse updateStatus(Integer subscriptionId, String status) {
        MemberPackage mp = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member package subscription not found: " + subscriptionId));

        String normalizedStatus = normalizeStatus(status);
        String previousStatus = mp.getStatus();
        if ("Pending".equalsIgnoreCase(previousStatus) && "Active".equals(normalizedStatus)) {
            throw new BusinessException("A Pending subscription can only be activated after its invoice is Paid");
        }
        mp.setStatus(normalizedStatus);
        MemberPackage saved = memberPackageRepository.save(mp);

        auditService.log(null, "CHANGE_SUBSCRIPTION_STATUS", "MEMBER_PACKAGE", saved.getSubscriptionId(),
                previousStatus + " -> " + normalizedStatus);
        return MemberPackageResponse.from(saved);
    }

    private String normalizeStatus(String status) {
        if ("Expired".equalsIgnoreCase(status)) return "Expired";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        return "Active";
    }
}
