package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.invoice.InvoiceService;
import com.sportscenter.invoice.InvoiceOrderResponse;
import com.sportscenter.notification.NotificationTemplates;
import com.sportscenter.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPackageService {
    private final java.time.Clock clock;
    private final MemberPackageRepository memberPackageRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final InvoiceService invoiceService;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    private final ApplicationEventPublisher eventPublisher;

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
        throw new BusinessException("LEGACY_MEMBERSHIP_RETIRED: Buy a Normal class or use Advanced registration instead of a shared membership");
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
        scheduleWriteLock.acquire(memberPackageRepository.findCenterIdById(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription center not found")));
        MemberPackage mp = memberPackageRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member package subscription not found: " + subscriptionId));

        if (!mp.isAdvanced()) throw new BusinessException("LEGACY_MEMBERSHIP_RETIRED: Shared membership state changes are retired");
        String normalizedStatus = normalizeStatus(status);
        String previousStatus = mp.getStatus();
        if(mp.isAdvanced()&&!"Active".equalsIgnoreCase(previousStatus)&&"Active".equals(normalizedStatus))
            throw new BusinessException("USE_INCIDENT_RECOVERY: Advanced reactivation requires incident and expiry evidence");
        if(mp.isAdvanced()&&!normalizedStatus.equalsIgnoreCase(previousStatus))throw new BusinessException("ADVANCED_STATUS_WORKFLOW_REQUIRED: Use payment, expiry, attendance completion or the verified incident workflow");
        if (!"Active".equalsIgnoreCase(previousStatus) && "Active".equals(normalizedStatus)) {
            invoiceService.requirePaidSubscription(subscriptionId);
            // Only the first successful payment grants credits. Reactivation never replenishes them.
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
        scheduleWriteLock.acquire(memberPackageRepository.findCenterIdById(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription center not found")));
        MemberPackage mp = memberPackageRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Member package subscription not found: " + subscriptionId));
        if (mp.isAdvanced()) throw new BusinessException("USE_INCIDENT_RECOVERY: Advanced extensions require verified incident evidence and an agreed makeup session");
        throw new BusinessException("LEGACY_MEMBERSHIP_RETIRED: Shared memberships cannot be extended");
    }

    @Transactional
    public int expireOutdatedPackages() {
        LocalDate today = LocalDate.now(clock);
        List<Integer> outdated = memberPackageRepository.findDueForExpiry(java.time.LocalDateTime.now(clock), today);
        int expiredCount = 0;
        for (Integer candidate : outdated) {
            Integer center=memberPackageRepository.findCenterIdById(candidate).orElse(null);
            if(center==null)continue;
            scheduleWriteLock.acquire(center);
            MemberPackage mp = memberPackageRepository.findByIdForUpdate(candidate).orElse(null);
            if (mp == null || !"Active".equals(mp.getStatus())) continue;
            if(mp.isAdvanced()&&(mp.getActivatedAt()==null||mp.getExpiresAt()==null))continue;
            boolean due = mp.getExpiresAt() != null ? !mp.getExpiresAt().isAfter(java.time.LocalDateTime.now(clock))
                    : mp.getEndDate() != null && mp.getEndDate().isBefore(today);
            if (!due) continue;
            mp.setExpiryRevokedCredit(mp.getRemainingCredit() == null ? 0 : mp.getRemainingCredit());
            mp.setExpiryProcessedAt(java.time.LocalDateTime.now(clock));
            mp.setStatus("Expired");
            mp.setRemainingCredit(0);
            memberPackageRepository.save(mp);
            expiredCount++;
            auditService.log(null, "EXPIRE_MEMBER_PACKAGE", "MEMBER_PACKAGE", mp.getSubscriptionId(),
                    "Package expired past " + mp.getEndDate() + ". Remaining credits wiped.");
            if (mp.getMembershipPackage() != null && mp.getMembershipPackage().isAdvanced()) {
                eventPublisher.publishEvent(NotificationTemplates.event(NotificationType.PACKAGE_EXPIRED,
                        List.of(mp.getMember().getId()),
                        NotificationTemplates.packageExpired(packageName(mp), mp.getEndDate()),
                        "MEMBER_PACKAGE", mp.getSubscriptionId(),
                        "PACKAGE_EXPIRED:" + mp.getSubscriptionId() + ":" + mp.getEndDate()));
            }
        }
        return expiredCount;
    }

    private String normalizeStatus(String status) {
        if ("Expired".equalsIgnoreCase(status)) return "Expired";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        if ("Active".equalsIgnoreCase(status)) return "Active";
        throw new BusinessException("Subscription status must be Active, Expired or Cancelled");
    }

    private String packageName(MemberPackage memberPackage) {
        return memberPackage.getMembershipPackage() == null ? "học" : memberPackage.getMembershipPackage().getName();
    }
}
