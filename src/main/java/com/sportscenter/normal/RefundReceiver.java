package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_refund_receivers") @Getter @Setter
public class RefundReceiver {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="refund_id",nullable=false) private Integer refundId;
    @Column(nullable=false,length=120) private String bank;
    @Column(name="account_number",nullable=false,length=40) private String accountNumber;
    @Column(name="account_name",nullable=false,length=150) private String accountName;
    @Column(name="confirmed_by",nullable=false) private Integer confirmedBy;
    @Column(name="confirmed_at",nullable=false) private LocalDateTime confirmedAt;
    @Column(name="verified_by") private Integer verifiedBy;
    @Column(name="verified_at") private LocalDateTime verifiedAt;
    @Column(name="verification_source",length=2000) private String verificationSource;
}
