package com.sportscenter.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingResultRepository extends JpaRepository<TrainingResult, Integer> {

    @Query("SELECT tr FROM TrainingResult tr " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH tr.member m " +
           "LEFT JOIN FETCH tr.coach c " +
           "WHERE (:sessionId IS NULL OR tr.session.id = :sessionId) " +
           "AND (:memberId IS NULL OR tr.member.id = :memberId) " +
           "AND (:coachId IS NULL OR tr.coach.id = :coachId) " +
           "ORDER BY tr.resultId DESC")
    List<TrainingResult> searchResults(
            @Param("sessionId") Integer sessionId,
            @Param("memberId") Integer memberId,
            @Param("coachId") Integer coachId
    );

    @Query("SELECT tr FROM TrainingResult tr " +
           "WHERE tr.session.id = :sessionId AND tr.member.id = :memberId")
    Optional<TrainingResult> findBySessionIdAndMemberId(
            @Param("sessionId") Integer sessionId,
            @Param("memberId") Integer memberId
    );
}
