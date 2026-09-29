package com.sportscenter.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "JOIN FETCH s.room r " +
           "WHERE (:classId IS NULL OR c.id = :classId) " +
           "  AND (:coachId IS NULL OR c.coach.id = :coachId) " +
           "  AND (:roomId IS NULL OR r.id = :roomId) " +
           "  AND (:startDate IS NULL OR s.sessionDate >= :startDate) " +
           "  AND (:endDate IS NULL OR s.sessionDate <= :endDate) " +
           "  AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<Session> findFiltered(@Param("classId") Integer classId,
                               @Param("coachId") Integer coachId,
                               @Param("roomId") Integer roomId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("status") String status);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "JOIN FETCH s.room r " +
           "WHERE s.id = :id")
    java.util.Optional<Session> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.coach " +
           "JOIN FETCH s.room r " +
           "WHERE r.id = :roomId " +
           "  AND s.sessionDate = :sessionDate " +
           "  AND s.status <> 'Cancelled' " +
           "  AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) " +
           "  AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Session> findRoomConflicts(@Param("roomId") Integer roomId,
                                    @Param("sessionDate") LocalDate sessionDate,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("excludeSessionId") Integer excludeSessionId);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.coach " +
           "JOIN FETCH s.room r " +
           "WHERE c.coach.id = :coachId " +
           "  AND s.sessionDate = :sessionDate " +
           "  AND s.status <> 'Cancelled' " +
           "  AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) " +
           "  AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Session> findCoachConflicts(@Param("coachId") Integer coachId,
                                     @Param("sessionDate") LocalDate sessionDate,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime,
                                     @Param("excludeSessionId") Integer excludeSessionId);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "JOIN FETCH s.room r " +
           "WHERE c.id IN (SELECT e.sportsClass.id FROM ClassEnrollment e WHERE e.member.id = :memberId AND e.status = 'Registered') " +
           "  AND (:startDate IS NULL OR s.sessionDate >= :startDate) " +
           "  AND (:endDate IS NULL OR s.sessionDate <= :endDate) " +
           "  AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<Session> findByEnrolledMember(@Param("memberId") Integer memberId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("status") String status);
    List<Session> findBySessionDateBetween(LocalDate startDate, LocalDate endDate);

    long countBySessionDate(LocalDate sessionDate);
}
