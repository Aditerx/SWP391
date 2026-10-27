package com.sportscenter.holiday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Integer id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 120)
    private String holidayName;

    @Column(name = "tag", nullable = false, length = 30)
    private String tag = "NATIONAL_FIXED";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "description")
    private String description;
    @Column(name="actual_closed_until") private java.time.LocalDateTime actualClosedUntil;

    public Holiday(LocalDate holidayDate, String holidayName, String tag, boolean isActive) {
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.tag = tag;
        this.isActive = isActive;
    }
}

