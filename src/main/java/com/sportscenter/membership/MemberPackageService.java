package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    public MemberPackageResponse subscribe(Integer memberId, MemberPackageRequest request) {
        User memberUser = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + memberId));

        MembershipPackage pkg = membershipPackageRepository.findById(request.packageId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found: " + request.packageId()));

        if ("Inactive".equalsIgnoreCase(pkg.getStatus())) {
            throw new BusinessException("Cannot subscribe to an Inactive membership package");
        }

        int days = request.durationDays() != null && request.durationDays() > 0
                ? request.durationDays()
                : (pkg.getDurationDays() != null ? pkg.getDurationDays() : 30);

        LocalDate startDate = request.startDate();
        boolean isRenewal = Boolean.TRUE.equals(request.isRenewal());

        // Find existing active subscriptions for renewal calculation
        List<MemberPackage> existingSubs = memberPackageRepository.findByMemberId(memberId);
        MemberPackage latestActiveSub = existingSubs.stream()
                .filter(s -> "Active".equalsIgnoreCase(s.getStatus()) && s.getEndDate() != null)
                .max(Comparator.comparing(MemberPackage::getEndDate))
                .orElse(null);

        if (startDate == null) {
            if (isRenewal && latestActiveSub != null && !latestActiveSub.getEndDate().isBefore(LocalDate.now())) {
                startDate = latestActiveSub.getEndDate().plusDays(1);
            } else {
                startDate = LocalDate.now();
            }
        }

        LocalDate endDate = startDate.plusDays(days);

        MemberPackage mp = new MemberPackage();
        mp.setMember(memberUser);
        mp.setMembershipPackage(pkg);
        mp.setStartDate(startDate);
        mp.setEndDate(endDate);
        mp.setStatus("Active");

        MemberPackage saved = memberPackageRepository.save(mp);

        // Auto-generate Invoice for this subscription
        try {
            invoiceService.createInvoiceForSubscription(
                    memberUser,
                    pkg,
                    null,
                    pkg.getPrice(),
                    request.paymentMethod() != null ? request.paymentMethod() : "Cash"
            );
        } catch (Exception ignored) {
            // Non-blocking invoice generation fallback
        }

        String action = isRenewal ? "RENEW_SUBSCRIPTION" : "SUBSCRIBE_PACKAGE";
        String detail = (isRenewal ? "Renewed " : "Subscribed to ") + pkg.getName() + " for Member #" + memberId +
                " (Valid: " + startDate + " to " + endDate + ")";
        if (request.paymentMethod() != null) {
            detail += " [Payment: " + request.paymentMethod() + "]";
        }

        auditService.log(null, action, "MEMBER_PACKAGE", saved.getSubscriptionId(), detail);
        return MemberPackageResponse.from(saved);
    }

    @Transactional
    public MemberPackageResponse updateStatus(Integer subscriptionId, String status) {
        MemberPackage mp = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member package subscription not found: " + subscriptionId));

        String normalizedStatus = normalizeStatus(status);
        String previousStatus = mp.getStatus();
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
