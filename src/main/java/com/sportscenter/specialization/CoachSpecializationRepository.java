package com.sportscenter.specialization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachSpecializationRepository extends JpaRepository<CoachSpecialization, CoachSpecializationId> {
    List<CoachSpecialization> findByCoachUserId(Integer userId);
    void deleteByCoachUserId(Integer userId);
}
