package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class BookingBlockService {
    private static final Logger log = LoggerFactory.getLogger(BookingBlockService.class);
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public void lift(Integer memberId, String reason, String actorEmail) {
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
        if (actor.getCenterId() != null && member.getUser() != null && member.getUser().getCenterId() != null
                && !actor.getCenterId().equals(member.getUser().getCenterId())) {
            throw new AccessDeniedException("Members may only be managed within the current center");
        }
        if (reason == null || reason.isBlank()) throw new BusinessException("Reason is required");
        member.setBookingBlockedUntil(null);
        memberRepository.save(member);
        auditService.log(actor.getId(), "BOOKING_BLOCK_LIFTED", "members", memberId, "Reason: " + reason);
        try { notificationService.send(memberId, "Booking restriction lifted", "Your class booking restriction has been lifted.", "System"); }
        catch (RuntimeException error) { log.warn("Unable to notify member {} about booking restriction lift: {}", memberId, error.getMessage()); }
    }
}
