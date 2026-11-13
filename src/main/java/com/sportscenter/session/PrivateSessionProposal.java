package com.sportscenter.session;

import com.sportscenter.membership.MemberPackage;
import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Immutable schedule offered to a Member; it reserves neither resources nor credit. */
@Entity
@Table(name="private_session_proposals", uniqueConstraints=@UniqueConstraint(columnNames={"subscription_id","request_key"}))
@Getter @Setter
public class PrivateSessionProposal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="subscription_id",nullable=false) private MemberPackage subscription;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="proposed_by",nullable=false) private User proposedBy;
    @Column(name="coach_id",nullable=false) private Integer coachId;
    @Column(name="room_id",nullable=false) private Integer roomId;
    @Column(name="class_id") private Integer classId;
    @Column(name="subject_id") private Integer subjectId;
    @Column(name="session_date",nullable=false) private LocalDate sessionDate;
    @Column(name="start_time",nullable=false) private LocalTime startTime;
    @Column(name="end_time",nullable=false) private LocalTime endTime;
    @Column(name="request_key",nullable=false,length=80) private String requestKey;
    @Column(name="request_hash",nullable=false,length=64) private String requestHash;
    @Column(nullable=false,length=20) private String status="Pending";
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="respond_by",nullable=false) private LocalDateTime respondBy;
    @Column(name="responded_at") private LocalDateTime respondedAt;
    @Column(name="support_reason",length=2000) private String supportReason;
    @Column(name="incident_id") private Integer incidentId;
    @Column(name="extra_validity_reason",length=2000) private String extraValidityReason;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",unique=true) private Session session;

    PrivateBookingRequest booking() {
        return new PrivateBookingRequest(subscription.getSubscriptionId(),coachId,roomId,sessionDate,startTime,endTime,
                classId,subjectId,true,"proposal:"+id,supportReason,incidentId,extraValidityReason);
    }
}
