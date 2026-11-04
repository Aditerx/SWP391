package com.sportscenter.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Original pay request identity. Never contains a secret or a complete payment URL. */
@Entity
@Table(name = "vnpay_payment_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uq_vnpay_attempt_request", columnNames = {"invoice_id", "tmn_code", "txn_ref", "transaction_date"}))
@Getter @Setter @NoArgsConstructor
public class VnPayPaymentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false) private Invoice invoice;
    @Column(name = "tmn_code", nullable = false, length = 32, updatable = false) private String tmnCode;
    @Column(name = "txn_ref", nullable = false, length = 100, updatable = false) private String txnRef;
    @Column(name = "transaction_date", nullable = false, length = 14, updatable = false) private String transactionDate;
    @Column(name = "amount_minor", nullable = false, updatable = false) private long amountMinor;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "last_query_at") private LocalDateTime lastQueryAt;

    public Snapshot snapshot() { return new Snapshot(id, tmnCode, txnRef, transactionDate, amountMinor); }
    public record Snapshot(Long id, String tmnCode, String txnRef, String transactionDate, long amountMinor) { }
}
