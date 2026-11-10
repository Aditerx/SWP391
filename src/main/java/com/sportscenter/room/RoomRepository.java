package com.sportscenter.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    @Query("select r.centerId from Room r where r.id=:id")
    java.util.Optional<Integer> findCenterIdById(@Param("id") Integer id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id=:id")
    java.util.Optional<Room> findByIdForUpdate(@Param("id") Integer id);
    @Query(value = """
            SELECT s.session_id AS sessionId,
                   s.session_date AS sessionDate,
                   s.start_time AS startTime,
                   c.class_name AS className
            FROM sessions s
            JOIN classes c ON c.class_id = s.class_id
            WHERE s.room_id = :roomId
              AND s.status = 'Scheduled'
              AND s.session_date >= :today
            ORDER BY s.session_date, s.start_time
            """, nativeQuery = true)
    List<RoomScheduleConflict> findFutureScheduledSessions(@Param("roomId") Integer roomId, @Param("today") java.time.LocalDate today);
}
