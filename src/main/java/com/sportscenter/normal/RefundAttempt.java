package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="normal_refund_attempts") @Getter @Setter
public class RefundAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="refund_id",nullable=false) private Integer refundId;
    @Column(name="receiver_id",nullable=false) private Integer receiverId;
    @Column(name="claim_key",nullable=false,unique=true,length=36) private String claimKey;
    @Column(name="claimed_by",nullable=false) private Integer claimedBy;
    @Column(name="claimed_at",nullable=false) private LocalDateTime claimedAt;
    @Column(nullable=false,length=30) private String status="Claimed";
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency="VND";
    @Column(name="bank_reference",unique=true,length=120) private String bankReference;
    @Column(name="performed_at") private LocalDateTime performedAt;
    @Column(name="verified_by") private Integer verifiedBy;
    @Column(name="verified_at") private LocalDateTime verifiedAt;
    @Column(name="bank_source",length=2000) private String bankSource;
    @Column(name="outcome_note",length=2000) private String outcomeNote;
}
