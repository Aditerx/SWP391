package com.sportscenter.specialization;

import com.sportscenter.user.Coach;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coach_specializations")
@Getter
@Setter
@NoArgsConstructor
public class CoachSpecialization {
    @EmbeddedId
    private CoachSpecializationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("coachId")
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("specializationId")
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    public CoachSpecialization(Coach coach, Specialization specialization) {
        this.coach = coach;
        this.specialization = specialization;
        this.id = new CoachSpecializationId(coach.getUserId(), specialization.getId());
    }
}
