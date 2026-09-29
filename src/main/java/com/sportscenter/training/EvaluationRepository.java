package com.sportscenter.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    @Query("SELECT e FROM Evaluation e " +
           "LEFT JOIN FETCH e.member m " +
           "LEFT JOIN FETCH e.coach c " +
           "WHERE (:memberId IS NULL OR e.member.id = :memberId) " +
           "AND (:coachId IS NULL OR e.coach.id = :coachId) " +
           "ORDER BY e.evaluationDate DESC, e.evaluationId DESC")
    List<Evaluation> searchEvaluations(
            @Param("memberId") Integer memberId,
            @Param("coachId") Integer coachId
    );
}
