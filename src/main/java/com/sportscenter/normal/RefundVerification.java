package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_refund_verifications") @Getter @Setter
public class RefundVerification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="attempt_id",nullable=false) private Integer attemptId;
    @Column(name="verified_by",nullable=false) private Integer verifiedBy;
    @Column(name="verified_at",nullable=false) private LocalDateTime verifiedAt;
    @Column(nullable=false,length=20) private String outcome;
    @Column(name="bank_source",nullable=false,length=2000) private String bankSource;
    @Column(nullable=false,length=2000) private String note;
}
