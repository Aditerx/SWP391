package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReceptionistRepository extends JpaRepository<Receptionist, Integer> {
    @Query("SELECT r FROM Receptionist r JOIN FETCH r.user u JOIN FETCH u.role role")
    List<Receptionist> findAllWithUser();
}
