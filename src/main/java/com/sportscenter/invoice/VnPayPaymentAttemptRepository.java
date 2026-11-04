package com.sportscenter.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VnPayPaymentAttemptRepository extends JpaRepository<VnPayPaymentAttempt, Long> {
    List<VnPayPaymentAttempt> findByInvoiceInvoiceIdOrderByIdDesc(Integer invoiceId);
    boolean existsByInvoiceInvoiceIdAndTmnCodeAndTxnRefAndTransactionDate(
            Integer invoiceId, String tmnCode, String txnRef, String transactionDate);
}
