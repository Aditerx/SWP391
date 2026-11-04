package com.sportscenter.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.List;

/** Deliberately not transactional: no database lock is held across an HTTP request. */
@Slf4j @Service @RequiredArgsConstructor
public class VnPayVerificationService {
    private final InvoiceService invoices;
    private final VnPayQueryClient client;

    public VnPayVerificationResponse verify(Integer id, Authentication auth) {
        Claim claim = invoices.claimVnpayVerification(id, auth);
        if (claim.response() != null) return claim.response();
        boolean unavailable = false;
        for (VnPayPaymentAttempt.Snapshot attempt : claim.attempts()) {
            try {
                VerifiedVnPayQuery result = client.query(attempt);
                if (result.successful()) return invoices.settleVnpayQuery(id, auth, attempt.id(), result);
            } catch (VnPayQueryException ex) {
                unavailable = true;
                log.warn("VNPay QueryDR invoice={} attempt={} reason={}", id, attempt.id(), ex.getMessage());
            }
        }
        if (unavailable)
            return invoices.vnpayVerificationState(id, auth, "QUERY_UNAVAILABLE", "Chưa xác minh được kết quả VNPay. Vui lòng kiểm tra lại sau.", 30);
        return invoices.vnpayVerificationState(id, auth, "NOT_CONFIRMED", "VNPay chưa xác nhận thanh toán; chưa nên thanh toán lại giao dịch.", 30);
    }

    public record Claim(VnPayVerificationResponse response, List<VnPayPaymentAttempt.Snapshot> attempts) { }
}
