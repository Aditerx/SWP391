package com.sportscenter.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
@NoArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "action")
    private String action;

    @Column(name = "target_entity")
    private String targetEntity;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "detail", length = 8000)
    private String detail;

    @Column(name = "timestamp")
    private LocalDateTime createdAt;

    /** Immutable event-time provenance. Null means legacy/unknown, never inferred at read time. */
    @Column(name = "audit_category", length = 24)
    private String category;

    @Column(name = "center_id")
    private Integer centerId;

    @Column(name = "scope_source", length = 40)
    private String scopeSource;

}
