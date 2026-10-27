package com.sportscenter.session;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Entity @Table(name="session_absence_notices") @Getter @Setter
public class SessionAbsenceNotice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private Session session;
    @Column(name="member_id",nullable=false) private Integer memberId;
    @Column(name="reported_at",nullable=false) private LocalDateTime reportedAt;
    @Column(name="original_start",nullable=false) private LocalDateTime originalStart;
    @Column(name="original_end",nullable=false) private LocalDateTime originalEnd;
    @Column(length=2000,nullable=false) private String reason;
    @Column(nullable=false) private boolean active=true;
    @Column(name="superseded_at") private LocalDateTime supersededAt;
}
