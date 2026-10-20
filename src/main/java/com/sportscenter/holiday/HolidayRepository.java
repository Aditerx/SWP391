package com.sportscenter.holiday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

    boolean existsByHolidayDateAndIsActiveTrue(LocalDate holidayDate);

    List<Holiday> findByIsActiveTrue();

    List<Holiday> findAllByOrderByHolidayDateAsc();

    List<Holiday> findByTagOrderByHolidayDateAsc(String tag);

    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.isActive = true AND h.holidayDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findActiveHolidayDatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

