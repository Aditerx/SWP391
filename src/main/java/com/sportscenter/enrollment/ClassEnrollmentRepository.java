package com.sportscenter.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Integer> {

    @Query("SELECT e FROM ClassEnrollment e " +
           "JOIN FETCH e.member m " +
           "JOIN FETCH e.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "WHERE c.id = :classId " +
           "ORDER BY e.enrolledAt DESC")
    List<ClassEnrollment> findByClassIdWithDetails(@Param("classId") Integer classId);

    @Query("SELECT e FROM ClassEnrollment e " +
           "JOIN FETCH e.member m " +
           "JOIN FETCH e.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "WHERE m.id = :memberId " +
           "ORDER BY e.enrolledAt DESC")
    List<ClassEnrollment> findByMemberIdWithDetails(@Param("memberId") Integer memberId);

    @Query("SELECT e FROM ClassEnrollment e " +
           "JOIN FETCH e.member m " +
           "JOIN FETCH e.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "WHERE (:classId IS NULL OR c.id = :classId) " +
           "  AND (:memberId IS NULL OR m.id = :memberId) " +
           "  AND (:status IS NULL OR e.status = :status) " +
           "ORDER BY e.enrolledAt DESC")
    List<ClassEnrollment> findFiltered(@Param("classId") Integer classId,
                                       @Param("memberId") Integer memberId,
                                       @Param("status") String status);

    Optional<ClassEnrollment> findByMemberIdAndSportsClassId(Integer memberId, Integer classId);

    boolean existsByMemberIdAndSportsClassIdAndStatusIn(Integer memberId, Integer classId, List<String> statuses);

    long countBySportsClassIdAndStatus(Integer classId, String status);

    boolean existsByMemberIdAndSportsClassIdAndStatus(Integer memberId, Integer classId, String status);
}
