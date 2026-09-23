package com.sportscenter.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {
    List<SystemLog> findAllByOrderByCreatedAtDesc();
}
