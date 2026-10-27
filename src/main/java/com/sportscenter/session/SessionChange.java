package com.sportscenter.session;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="session_changes",uniqueConstraints=@UniqueConstraint(columnNames={"session_id","request_key"})) @Getter @Setter
public class SessionChange {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private Session session;
    @Column(name="request_key",nullable=false,length=80) private String requestKey;
    @Column(name="request_hash",nullable=false,length=64) private String requestHash;
    @Column(name="actor_id",nullable=false) private Integer actorId;
    @Column(name="changed_at",nullable=false) private LocalDateTime changedAt;
    @Column(nullable=false,length=40) private String cause;
    @Column(nullable=false,length=2000) private String reason;
    @Column(name="member_agreed",nullable=false) private boolean memberAgreed;
    @Column(name="before_data",nullable=false,length=4000) private String beforeData;
    @Column(name="after_data",nullable=false,length=4000) private String afterData;
}
