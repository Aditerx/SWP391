package com.sportscenter.specialization;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CoachSpecializationId implements Serializable {
    @Column(name = "coach_id")
    private Integer coachId;

    @Column(name = "specialization_id")
    private Integer specializationId;
}
