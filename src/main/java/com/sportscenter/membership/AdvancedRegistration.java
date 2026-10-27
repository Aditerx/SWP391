package com.sportscenter.membership;

import com.sportscenter.user.User;
import com.sportscenter.subject.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "advanced_registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "request_key"}))
@Getter @Setter
public class AdvancedRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private User member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subject_id", nullable = false) private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coach_id", nullable = false) private User coach;
    @Column(name = "request_key", nullable = false, length = 80) private String requestKey;
    @Column(nullable = false, length = 20) private String status = "Requested";
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accepted_by") private User acceptedBy;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @Column(length = 2000) private String reason;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subscription_id", unique = true) private MemberPackage subscription;
}
