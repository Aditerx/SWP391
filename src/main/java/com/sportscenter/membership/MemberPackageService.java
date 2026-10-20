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
        int days = pkg.isAdvanced()
                ? (pkg.getDurationDays() != null ? pkg.getDurationDays() : 45)
                : (pkg.getDurationDays() != null ? pkg.getDurationDays() : 30);

        MemberPackage mp = new MemberPackage();
        mp.setMember(memberUser);
        mp.setMembershipPackage(pkg);
        mp.setStartDate(startDate);
        mp.setEndDate(startDate.plusDays(days));
        mp.setStatus("Pending");
        mp.setRemainingCredit(0);
        mp.setValidityDays(days);
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
        if (!"Active".equalsIgnoreCase(previousStatus) && "Active".equals(normalizedStatus)) {
            invoiceService.requirePaidSubscription(subscriptionId);
            if (mp.getMembershipPackage() != null && mp.getMembershipPackage().isAdvanced()
                    && (mp.getRemainingCredit() == null || mp.getRemainingCredit() == 0)) {
                int credits = mp.getMembershipPackage().getTotalCredit() != null ? mp.getMembershipPackage().getTotalCredit() : 12;
                mp.setRemainingCredit(credits);
            }
        }
        if ("Cancelled".equals(normalizedStatus) || "Expired".equals(normalizedStatus)) {
            invoiceService.cancelPendingSubscriptionPayment(subscriptionId);
            if ("Expired".equals(normalizedStatus)) {
                mp.setRemainingCredit(0);
            }
        }
        mp.setStatus(normalizedStatus);
        MemberPackage saved = memberPackageRepository.save(mp);

        auditService.log(null, "CHANGE_SUBSCRIPTION_STATUS", "MEMBER_PACKAGE", saved.getSubscriptionId(),
                previousStatus + " -> " + normalizedStatus);
        return MemberPackageResponse.from(saved);
    }

    @Transactional
    public MemberPackageResponse extendValidity(Integer subscriptionId, PackageExtensionRequest request, Authentication authentication) {
        if (request.extendDays() == null || request.extendDays() <= 0) {
            throw new BusinessException("Số ngày gia hạn phải lớn hơn 0");
        }

        MemberPackage mp = memberPackageRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member package subscription not found: " + subscriptionId));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate currentEnd = mp.getEndDate() != null ? mp.getEndDate() : today;
        LocalDate newEnd = currentEnd.plusDays(request.extendDays());
        int oldValidity = mp.getValidityDays() != null ? mp.getValidityDays() : 45;

        mp.setEndDate(newEnd);
        mp.setValidityDays(oldValidity + request.extendDays());

        if ("Expired".equalsIgnoreCase(mp.getStatus()) && newEnd.isAfter(today)) {
            mp.setStatus("Active");
        }

        MemberPackage saved = memberPackageRepository.save(mp);
        Integer actorId = null;
        if (authentication != null && authentication.getName() != null) {
            actorId = userRepository.findByEmailIgnoreCase(authentication.getName()).map(User::getId).orElse(null);
        }
        auditService.log(actorId, "EXTEND_PACKAGE_VALIDITY", "MEMBER_PACKAGE", saved.getSubscriptionId(),
                String.format("Gia hạn gói %d ngày (hạn mới: %s). Lý do: %s",
                        request.extendDays(), newEnd, request.reason() != null ? request.reason() : "N/A"));

        return MemberPackageResponse.from(saved);
    }

    @Transactional
    public int expireOutdatedPackages() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<MemberPackage> outdated = memberPackageRepository.findExpiredActivePackages(today);
        for (MemberPackage mp : outdated) {
            mp.setStatus("Expired");
            mp.setRemainingCredit(0);
            memberPackageRepository.save(mp);
            auditService.log(null, "EXPIRE_MEMBER_PACKAGE", "MEMBER_PACKAGE", mp.getSubscriptionId(),
                    "Package expired past " + mp.getEndDate() + ". Remaining credits wiped.");
        }
        return outdated.size();
    }

    private String normalizeStatus(String status) {
        if ("Expired".equalsIgnoreCase(status)) return "Expired";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        if ("Active".equalsIgnoreCase(status)) return "Active";
        throw new BusinessException("Subscription status must be Active, Expired or Cancelled");
    }
}
