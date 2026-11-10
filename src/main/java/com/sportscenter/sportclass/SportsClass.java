package com.sportscenter.sportclass;

import com.sportscenter.subject.Subject;
import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
public class SportsClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer id;

    @Column(name = "class_name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private User coach;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "min_capacity")
    private Integer minCapacity;
    @Column(name = "opening_confirmed_by")
    private Integer openingConfirmedBy;
    @Column(name = "opening_confirmed_at")
    private java.time.LocalDateTime openingConfirmedAt;
    @Column(name = "opening_reason", length = 2000)
    private String openingReason;
    @Column(name = "postponement_count", nullable = false)
    private int postponementCount;
    @Column(name = "lifecycle_version", nullable = false)
    private long lifecycleVersion;
    @Column(name = "change_pending", nullable = false)
    private boolean changePending;

    @Column(name = "tuition_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    /** New drafts only; historical classes retain their published price. */
    @Column(name = "pricing_draft", nullable = false)
    private boolean pricingDraft;

    @Column(name = "class_type", length = 20)
    private String classType = "Normal";

    @Column(name = "total_sessions")
    private Integer totalSessions = 12;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    private String status;

    @Column(name = "center_id", nullable = false)
    private Integer centerId = 1;

    public boolean isAdvanced() {
        return "Advanced".equalsIgnoreCase(classType);
    }

    public boolean isNormal() {
        return classType == null || "Normal".equalsIgnoreCase(classType);
    }
}
