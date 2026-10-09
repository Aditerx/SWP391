package com.sportscenter.feedback;

import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_feedbacks", uniqueConstraints = {
        @UniqueConstraint(name = "uq_feedback_class_member", columnNames = {"class_id", "member_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ClassFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportsClass sportsClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private User coach;

    @Column(name = "class_rating", nullable = false)
    private Short classRating;

    @Column(name = "coach_rating", nullable = false)
    private Short coachRating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
