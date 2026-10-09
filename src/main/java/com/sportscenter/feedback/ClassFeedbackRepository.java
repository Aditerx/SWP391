package com.sportscenter.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassFeedbackRepository extends JpaRepository<ClassFeedback, Integer> {
    boolean existsBySportsClassIdAndMemberId(Integer classId, Integer memberId);

    @Query("SELECT f FROM ClassFeedback f " +
            "JOIN FETCH f.sportsClass c " +
            "JOIN FETCH f.member m " +
            "JOIN FETCH f.coach coach " +
            "WHERE c.id = :classId AND m.id = :memberId")
    Optional<ClassFeedback> findForMemberAndClass(@Param("classId") Integer classId,
                                                   @Param("memberId") Integer memberId);

    @Query("SELECT f FROM ClassFeedback f " +
            "JOIN FETCH f.member m " +
            "JOIN FETCH f.coach coach " +
            "WHERE f.sportsClass.id = :classId " +
            "ORDER BY f.createdAt DESC, f.feedbackId DESC")
    List<ClassFeedback> findForClass(@Param("classId") Integer classId);

    @Query("SELECT f FROM ClassFeedback f " +
            "JOIN FETCH f.sportsClass c " +
            "JOIN FETCH f.coach coach " +
            "WHERE coach.id = :coachId " +
            "AND (:classId IS NULL OR c.id = :classId) " +
            "ORDER BY f.createdAt DESC, f.feedbackId DESC")
    List<ClassFeedback> findForCoach(@Param("coachId") Integer coachId,
                                      @Param("classId") Integer classId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Attendance a " +
            "WHERE a.member.id = :memberId " +
            "AND a.state = 'Present' " +
            "AND a.session.sportsClass.id = :classId")
    boolean hasPresentAttendance(@Param("classId") Integer classId,
                                 @Param("memberId") Integer memberId);
}
