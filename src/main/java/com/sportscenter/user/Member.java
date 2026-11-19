package com.sportscenter.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "goal")
    private String goal;

    @Column(name = "health_note")
    private String healthNote;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "checkin_secret", length = 64)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String checkinSecret;
    @Column(name = "checkin_last_step")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long checkinLastStep;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="checkin_code_nonce") private Long checkinCodeNonce;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="checkin_code_issued_at") private LocalDateTime checkinCodeIssuedAt;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="checkin_code_expires_at") private LocalDateTime checkinCodeExpiresAt;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="checkin_code_used_at") private LocalDateTime checkinCodeUsedAt;

    @Column(name = "booking_blocked_until")
    private LocalDateTime bookingBlockedUntil;
}
