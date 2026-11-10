package com.sportscenter.normal;
import com.sportscenter.invoice.PaymentResolution;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_refunds") @Getter @Setter
public class NormalRefund {
    @Id private Integer id;
    @MapsId @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="id") private PaymentResolution resolution;
    @Column(name="class_id",nullable=false) private Integer classId;
    @Column(nullable=false,length=30) private String origin;
    @Column(name="receiver_id") private Integer receiverId;
    @Column(name="approved_receiver_id") private Integer approvedReceiverId;
    @Column(nullable=false) private long version;
    @Column(name="due_at") private LocalDateTime dueAt;
    @Column(name="first_due_at") private LocalDateTime firstDueAt;
    @Column(name="issue_note",length=2000) private String issueNote;
}
