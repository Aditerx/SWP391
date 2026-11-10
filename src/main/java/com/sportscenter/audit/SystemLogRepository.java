package com.sportscenter.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<SystemLog> {
    List<SystemLog> findAllByOrderByCreatedAtDesc();
}
