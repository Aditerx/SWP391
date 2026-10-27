package com.sportscenter.membership;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "member_packages")
@Getter
@Setter
@NoArgsConstructor
public class MemberPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Integer subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private MembershipPackage membershipPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private com.sportscenter.subject.Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_coach_id")
    private User primaryCoach;
    @Column(name="purchase_amount",precision=12,scale=2) private java.math.BigDecimal purchaseAmount;

    @Column(name = "activated_at")
    private java.time.LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    @Column(name = "expiry_revoked_credit")
    private Integer expiryRevokedCredit = 0;

    @Column(name = "expiry_processed_at")
    private java.time.LocalDateTime expiryProcessedAt;
    @Column(name="deferred_credit_return",nullable=false) private int deferredCreditReturn;

    @Column(name = "approved_extra_seconds", nullable = false)
    private long approvedExtraSeconds;
    @Column(name="approved_extra_nanos",nullable=false) private int approvedExtraNanos;

    public boolean isAdvanced() {
        return subject != null || membershipPackage != null && membershipPackage.isAdvanced();
    }

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "remaining_credit")
    private Integer remainingCredit = 0;

    @Column(name = "validity_days")
    private Integer validityDays = 45;

    @Column(name = "status")
    private String status;
}
