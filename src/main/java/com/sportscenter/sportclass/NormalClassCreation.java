package com.sportscenter.sportclass;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "normal_class_requests")
@Getter @Setter
public class NormalClassCreation {
    @Id @Column(name = "request_id", length = 36)
    private String requestId;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "center_id", nullable = false)
    private Integer centerId;
    @OneToOne @JoinColumn(name = "class_id", nullable = false)
    private SportsClass sportsClass;
}
