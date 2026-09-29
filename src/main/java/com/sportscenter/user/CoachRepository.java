package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Integer> {
    @Query("SELECT c FROM Coach c JOIN FETCH c.user u JOIN FETCH u.role r")
    List<Coach> findAllWithUser();
}
