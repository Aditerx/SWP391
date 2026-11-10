package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_transfers",uniqueConstraints=@UniqueConstraint(columnNames={"proposal_id","invoice_id"})) @Getter @Setter
public class NormalTransfer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="proposal_id",nullable=false) private Integer proposalId;
    @Column(name="invoice_id",nullable=false) private Integer invoiceId;
    @Column(name="source_enrollment_id",nullable=false) private Integer sourceEnrollmentId;
    @Column(name="target_enrollment_id",nullable=false) private Integer targetEnrollmentId;
    @Column(name="consented_by",nullable=false) private Integer consentedBy;
    @Column(name="transferred_at",nullable=false) private LocalDateTime transferredAt;
}
