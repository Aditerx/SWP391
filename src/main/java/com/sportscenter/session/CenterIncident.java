package com.sportscenter.session;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name="center_incidents",uniqueConstraints=@UniqueConstraint(columnNames={"session_id","request_key"})) @Getter @Setter
public class CenterIncident {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private Session session;
    @Column(name="request_key",nullable=false,length=80) private String requestKey;
    @Column(name="started_at",nullable=false) private LocalDateTime startedAt;
    @Column(name="ended_at") private LocalDateTime endedAt;
    @Column(nullable=false,length=2000) private String reason;
    @Column(nullable=false,length=2000) private String evidence;
    @Column(nullable=false,length=30) private String status="WaitingForRecovery";
    @Column(name="created_by",nullable=false) private Integer createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="resolved_by") private Integer resolvedBy;
    @Column(name="resolved_at") private LocalDateTime resolvedAt;
    @Column(name="original_deadline") private LocalDateTime originalDeadline;
    @Column(name="deadline_before") private LocalDateTime deadlineBefore;
    @Column(name="deadline_after") private LocalDateTime deadlineAfter;
    @Column(name="recovered_credits",nullable=false) private int recoveredCredits;
    @Column(name="session_compensated",nullable=false) private boolean sessionCompensated;
    @ElementCollection @CollectionTable(name="incident_affected_members",joinColumns=@JoinColumn(name="incident_id"))
    @Column(name="member_id",nullable=false) private Set<Integer> affectedMembers=new HashSet<>();
}
