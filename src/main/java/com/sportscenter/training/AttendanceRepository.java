package com.sportscenter.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.session s " +
           "LEFT JOIN FETCH a.member m " +
           "LEFT JOIN FETCH a.recordedBy r " +
           "WHERE (:sessionId IS NULL OR (a.session IS NOT NULL AND a.session.id = :sessionId)) " +
           "AND (:memberId IS NULL OR a.member.id = :memberId) " +
           "AND (:recordedBy IS NULL OR (a.recordedBy IS NOT NULL AND a.recordedBy.id = :recordedBy)) " +
           "AND (:state IS NULL OR LOWER(a.state) = LOWER(:state)) " +
           "AND (:startDate IS NULL OR a.checkInTime >= :startDate) " +
           "AND (:endDate IS NULL OR a.checkInTime <= :endDate) " +
           "ORDER BY a.attendanceId DESC")
    List<Attendance> searchAttendances(
            @Param("sessionId") Integer sessionId,
            @Param("memberId") Integer memberId,
            @Param("recordedBy") Integer recordedBy,
            @Param("state") String state,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT a FROM Attendance a " +
           "WHERE a.session.id = :sessionId AND a.member.id = :memberId")
    Optional<Attendance> findBySessionIdAndMemberId(
            @Param("sessionId") Integer sessionId,
            @Param("memberId") Integer memberId
    );

    @Query("SELECT a FROM Attendance a " +
           "WHERE a.session IS NULL AND a.member.id = :memberId AND a.checkOutTime IS NULL " +
           "ORDER BY a.attendanceId DESC")
    List<Attendance> findOpenCheckInsByMemberId(@Param("memberId") Integer memberId);
}
