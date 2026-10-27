package com.sportscenter.membership;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "coach_work_schedules") @Getter @Setter
public class CoachWorkSchedule {
    @Id @Column(name = "coach_id") private Integer coachId;
    @OneToOne(fetch = FetchType.LAZY) @MapsId @JoinColumn(name = "coach_id") private User coach;
    @Column(name = "accepting_new", nullable = false) private boolean acceptingNew;
    @ElementCollection @CollectionTable(name = "coach_work_slots", joinColumns = @JoinColumn(name = "coach_id"))
    @Column(name = "slot_key", length = 20, nullable = false) private Set<String> weeklySlots = new HashSet<>();
    @ElementCollection @CollectionTable(name = "coach_days_off", joinColumns = @JoinColumn(name = "coach_id"))
    @Column(name = "day_off", nullable = false) private Set<LocalDate> daysOff = new HashSet<>();
    public boolean works(LocalDate date, int slot) {
        return !daysOff.contains(date) && weeklySlots.contains(date.getDayOfWeek().name() + ":" + slot);
    }
}
