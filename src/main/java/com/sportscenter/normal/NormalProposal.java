package com.sportscenter.normal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="normal_proposals") @Getter @Setter
public class NormalProposal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="class_id",nullable=false) private Integer classId;
    @Column(name="target_class_id") private Integer targetClassId;
    @Column(nullable=false,length=20) private String kind;
    @Column(nullable=false,length=20) private String status="Pending";
    @Column(name="shift_days") private Integer shiftDays;
    @Column(nullable=false) private long version;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(name="schedule_json",nullable=false,columnDefinition="TEXT") private String scheduleJson;
    @Column(nullable=false,length=2000) private String reason;
    @Column(name="created_by",nullable=false) private Integer createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
