package com.sportscenter.specialization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CoachSpecializationRepository extends JpaRepository<CoachSpecialization, CoachSpecializationId> {
    List<CoachSpecialization> findByCoachUserId(Integer userId);
    @Query("SELECT cs FROM CoachSpecialization cs JOIN FETCH cs.specialization " +
            "WHERE cs.id.coachId IN :coachIds")
    List<CoachSpecialization> findAllWithSpecializationsByCoachIds(@Param("coachIds") Collection<Integer> coachIds);
    void deleteByCoachUserId(Integer userId);
}
