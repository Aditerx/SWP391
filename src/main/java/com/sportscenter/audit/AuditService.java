package com.sportscenter.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final java.time.Clock clock;
    private final SystemLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Integer userId, String action, String targetEntity, Integer targetId, String detail) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(java.time.LocalDateTime.now(clock));
        repository.save(log);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Integer userId, String action, String targetEntity, Integer targetId, String detail, LocalDateTime occurredAt) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(occurredAt);
        repository.save(log);
    }
}
