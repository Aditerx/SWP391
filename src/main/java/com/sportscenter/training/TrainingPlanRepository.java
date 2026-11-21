package com.sportscenter.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Integer> {

    @Query("SELECT tp FROM TrainingPlan tp " +
           "LEFT JOIN FETCH tp.coach c " +
           "LEFT JOIN FETCH tp.sportsClass cl " +
           "LEFT JOIN FETCH tp.member m " +
           "ORDER BY tp.planId DESC")
    List<TrainingPlan> findAllWithDetails();

    @Query("SELECT tp FROM TrainingPlan tp " +
           "LEFT JOIN FETCH tp.coach c " +
           "LEFT JOIN FETCH tp.sportsClass cl " +
           "LEFT JOIN FETCH tp.member m " +
           "WHERE (:coachId IS NULL OR tp.coach.id = :coachId) " +
           "AND (:classId IS NULL OR (tp.sportsClass IS NOT NULL AND tp.sportsClass.id = :classId)) " +
           "AND (:memberId IS NULL OR (tp.member IS NOT NULL AND tp.member.id = :memberId)) " +
           "ORDER BY tp.planId DESC")
    List<TrainingPlan> searchPlans(
            @Param("coachId") Integer coachId,
            @Param("classId") Integer classId,
            @Param("memberId") Integer memberId
    );

    @Query("SELECT tp FROM TrainingPlan tp " +
            "LEFT JOIN FETCH tp.coach c LEFT JOIN FETCH tp.sportsClass cl LEFT JOIN FETCH tp.member m " +
            "WHERE (:coachId IS NULL OR c.id=:coachId) AND (:classId IS NULL OR cl.id=:classId) " +
            "AND (m.id=:memberId OR (tp.member IS NULL AND (cl.classType='Normal' OR cl.classType IS NULL) " +
            "AND EXISTS (SELECT e.id FROM ClassEnrollment e WHERE e.sportsClass=tp.sportsClass " +
            "AND e.member.id=:memberId AND e.status IN ('Registered','Completed')) " +
            "AND EXISTS (SELECT i.invoiceId FROM Invoice i WHERE coalesce(i.entitlementClass.id,i.sportsClass.id)=tp.sportsClass.id " +
            "AND i.member.id=:memberId AND i.paymentStatus='Paid' " +
            "AND i.reconciliationRequired=false AND i.cashConfirmationReversed=false " +
            "AND NOT EXISTS (SELECT r.id FROM PaymentResolution r WHERE r.invoice=i AND r.type='Refund' AND r.status='Completed')))) ORDER BY tp.planId DESC")
    List<TrainingPlan> searchMemberPlans(@Param("coachId") Integer coachId,
            @Param("classId") Integer classId, @Param("memberId") Integer memberId);
}
