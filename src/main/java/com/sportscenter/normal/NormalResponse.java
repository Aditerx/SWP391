package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_responses",uniqueConstraints=@UniqueConstraint(columnNames={"proposal_id","invoice_id"})) @Getter @Setter
public class NormalResponse {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="proposal_id",nullable=false) private Integer proposalId;
    @Column(name="invoice_id",nullable=false) private Integer invoiceId;
    @Column(name="member_id",nullable=false) private Integer memberId;
    @Column(nullable=false,length=20) private String choice;
    @Column(nullable=false) private long version;
    @Column(name="responded_at",nullable=false) private LocalDateTime respondedAt;
}
