package com.sportscenter.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Integer> {
    @Query("select s.sportsClass.centerId from Session s where s.id = :id")
    java.util.Optional<Integer> findCenterIdById(@Param("id") Integer id);
    long countBySportsClassIdAndStatusNot(Integer classId, String status);
    long countBySportsClassIdAndStatusAndAttendanceFinalizedFalse(Integer classId, String status);
    long countByMemberPackageSubscriptionIdAndStatusAndAttendanceFinalizedFalse(Integer subscriptionId, String status);
    boolean existsBySportsClassSubjectIdAndStatusAndSessionDateGreaterThanEqual(Integer subjectId, String status, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.id = :id")
    java.util.Optional<Session> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "LEFT JOIN FETCH s.coach " +
           "LEFT JOIN FETCH s.bookedMember " +
           "LEFT JOIN FETCH s.memberPackage " +
           "JOIN FETCH s.room r " +
           "WHERE (:classId IS NULL OR c.id = :classId) " +
           "  AND (:coachId IS NULL OR s.coach.id = :coachId OR (s.coach IS NULL AND c.coach.id = :coachId)) " +
           "  AND (:roomId IS NULL OR r.id = :roomId) " +
           "  AND (CAST(:startDate AS date) IS NULL OR s.sessionDate >= :startDate) " +
           "  AND (CAST(:endDate AS date) IS NULL OR s.sessionDate <= :endDate) " +
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
           "LEFT JOIN FETCH s.coach " +
           "LEFT JOIN FETCH s.bookedMember " +
           "LEFT JOIN FETCH s.memberPackage " +
           "JOIN FETCH s.room r " +
           "WHERE s.id = :id")
    java.util.Optional<Session> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.coach " +
           "LEFT JOIN FETCH s.coach " +
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
           "LEFT JOIN FETCH s.coach " +
           "JOIN FETCH s.room r " +
           "WHERE (s.coach.id = :coachId OR (s.coach IS NULL AND c.coach.id = :coachId)) " +
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
           "LEFT JOIN FETCH c.coach " +
           "LEFT JOIN FETCH s.coach " +
           "JOIN FETCH s.room r " +
           "WHERE (s.bookedMember.id = :memberId " +
           "   OR c.id IN (SELECT e.sportsClass.id FROM ClassEnrollment e WHERE e.member.id = :memberId AND e.status IN ('Registered', 'Pending'))) " +
           "  AND s.sessionDate = :sessionDate " +
           "  AND s.status <> 'Cancelled' " +
           "  AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) " +
           "  AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Session> findMemberConflicts(@Param("memberId") Integer memberId,
                                      @Param("sessionDate") LocalDate sessionDate,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime,
                                      @Param("excludeSessionId") Integer excludeSessionId);

    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.sportsClass c " +
           "LEFT JOIN FETCH c.subject " +
           "LEFT JOIN FETCH c.coach " +
           "LEFT JOIN FETCH s.coach " +
           "LEFT JOIN FETCH s.bookedMember " +
           "JOIN FETCH s.room r " +
           "WHERE (c.id IN (SELECT e.sportsClass.id FROM ClassEnrollment e WHERE e.member.id = :memberId AND e.status IN ('Registered', 'Completed')) " +
           "   OR s.bookedMember.id = :memberId) " +
           "  AND (CAST(:startDate AS date) IS NULL OR s.sessionDate >= :startDate) " +
           "  AND (CAST(:endDate AS date) IS NULL OR s.sessionDate <= :endDate) " +
           "  AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<Session> findByEnrolledMember(@Param("memberId") Integer memberId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("status") String status);

    List<Session> findBySessionDateBetween(LocalDate startDate, LocalDate endDate);

    long countBySessionDate(LocalDate sessionDate);

    @Query("SELECT s FROM Session s WHERE s.sportsClass.id = :classId AND s.status <> 'Cancelled' " +
            "AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime >= :nowTime)) " +
            "ORDER BY s.sessionDate, s.startTime")
    List<Session> findNextUpcomingForClass(@Param("classId") Integer classId,
            @Param("today") LocalDate today, @Param("nowTime") LocalTime nowTime);

    @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.sportsClass c " +
            "WHERE (s.bookedMember.id = :memberId OR EXISTS (SELECT e.id FROM ClassEnrollment e WHERE e.sportsClass.id = c.id " +
            "AND e.member.id = :memberId AND e.status IN ('Registered', 'Pending'))) " +
            "AND c.id <> :targetClassId AND s.status <> 'Cancelled' " +
            "AND (s.sessionDate > :today OR (s.sessionDate = :today AND s.startTime >= :nowTime)) " +
            "AND EXISTS (SELECT target.id FROM Session target WHERE target.sportsClass.id = :targetClassId " +
            "AND target.status <> 'Cancelled' AND (target.sessionDate > :today OR " +
            "(target.sessionDate = :today AND target.startTime >= :nowTime)) " +
            "AND target.sessionDate = s.sessionDate AND s.startTime < target.endTime AND s.endTime > target.startTime) " +
            "ORDER BY s.sessionDate, s.startTime")
    List<Session> findFutureEnrollmentScheduleConflicts(@Param("memberId") Integer memberId,
            @Param("targetClassId") Integer targetClassId, @Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime);

    @Query("SELECT s FROM Session s WHERE s.attendanceFinalized = false AND s.status <> 'Cancelled' " +
            "AND (s.sessionDate > :cutoffDate OR (s.sessionDate = :cutoffDate AND s.endTime >= :cutoffTime)) " +
            "AND (s.sessionDate < :today OR (s.sessionDate = :today AND s.endTime <= :nowTime))")
    List<Session> findSessionsReadyForAttendanceFinalization(@Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime, @Param("cutoffDate") LocalDate cutoffDate,
            @Param("cutoffTime") LocalTime cutoffTime);
}
