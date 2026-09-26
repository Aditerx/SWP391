package com.sportscenter.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    @Query(value = """
            SELECT s.session_id AS sessionId,
                   s.session_date AS sessionDate,
                   s.start_time AS startTime,
                   c.class_name AS className
            FROM sessions s
            JOIN classes c ON c.class_id = s.class_id
            WHERE s.room_id = :roomId
              AND s.status = N'Scheduled'
              AND s.session_date >= CAST(GETDATE() AS date)
            ORDER BY s.session_date, s.start_time
            """, nativeQuery = true)
    List<RoomScheduleConflict> findFutureScheduledSessions(@Param("roomId") Integer roomId);
}
