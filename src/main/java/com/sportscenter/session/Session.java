package com.sportscenter.session;

import com.sportscenter.membership.MemberPackage;
import com.sportscenter.room.Room;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportsClass sportsClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private User coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_member_id")
    private User bookedMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private MemberPackage memberPackage;

    @Column(name = "booking_key", length = 80)
    private String bookingKey;
    @Column(name="booking_hash",length=64) private String bookingHash;
    @Column(name = "created_by") private Integer createdBy;
    @Column(name = "agreement_confirmed_at") private java.time.LocalDateTime agreementConfirmedAt;
    @Column(name = "support_reason", length = 2000) private String supportReason;
    @Column(name = "credit_returned", nullable = false) private boolean creditReturned;
    @Column(name = "center_invalidated", nullable = false) private boolean centerInvalidated;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "makeup_for_session_id") private Session makeupFor;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "status", nullable = false)
    private String status = "Scheduled";

    @Column(name = "attendance_finalized", nullable = false)
    private boolean attendanceFinalized;

    public boolean isPrivateSession() {
        return bookedMember != null || (sportsClass != null && sportsClass.isAdvanced());
    }
}
