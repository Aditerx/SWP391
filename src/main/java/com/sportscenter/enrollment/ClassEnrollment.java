package com.sportscenter.enrollment;

import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_enrollments")
@Getter
@Setter
@NoArgsConstructor
public class ClassEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportsClass sportsClass;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="eligibility_subscription_id")
    private com.sportscenter.membership.MemberPackage eligibilitySubscription;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "status", nullable = false)
    private String status = "Registered";

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;
    @Column(name = "cancel_reason")
    private String cancelReason;
    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;
}
