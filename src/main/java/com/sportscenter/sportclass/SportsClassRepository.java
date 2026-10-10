package com.sportscenter.sportclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SportsClassRepository extends JpaRepository<SportsClass, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SportsClass c WHERE c.id = :id")
    Optional<SportsClass> findByIdForUpdate(@Param("id") Integer id);
}
