package com.sportscenter.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final SystemLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Integer userId, String action, String targetEntity, Integer targetId, String detail) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetail(detail);
        repository.save(log);
    }
}
