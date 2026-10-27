package com.sportscenter.invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="payment_resolutions",uniqueConstraints=@UniqueConstraint(columnNames={"invoice_id","resolution_type"})) @Getter @Setter
public class PaymentResolution {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id",nullable=false) private Invoice invoice;
    @Column(name="resolution_type",nullable=false,length=30) private String type;
    @Column(nullable=false,length=30) private String status="Open";
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=2000) private String reason;
    @Column(length=2000) private String evidence;
    @Column(name="outcome_note",length=2000) private String outcomeNote;
    @Column(name="created_by",nullable=false) private Integer createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="approved_by") private Integer approvedBy;
    @Column(name="approved_at") private LocalDateTime approvedAt;
    @Column(name="completed_by") private Integer completedBy;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="performed_at") private LocalDateTime performedAt;
}
