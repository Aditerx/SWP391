package com.sportscenter.audit;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuditQueryService {
    private final ActorPolicy actors;
    private final SystemLogRepository logs;

    @Transactional(readOnly = true)
    public Page<SystemLog> search(Authentication auth, LocalDate from, LocalDate to, String action,
                                  String targetEntity, Integer actorId, int page, int size) {
        var actor = actors.actor(auth);
        if (auth.getAuthorities().stream().noneMatch(a -> "VIEW_AUDIT_LOG".equals(a.getAuthority())))
            throw new AccessDeniedException("VIEW_AUDIT_LOG required");
        boolean admin = ActorPolicy.role(actor, "Admin");
        if (!admin && !ActorPolicy.role(actor, "CenterManager"))
            throw new AccessDeniedException("Admin or CenterManager required");
        if (!admin && actor.getCenterId() == null) throw new AccessDeniedException("Manager center required");
        if (page < 0 || size < 1 || size > 100 || (long)page * size > Integer.MAX_VALUE || from != null && to != null && to.isBefore(from)
                || LocalDate.MAX.equals(to) || actorId != null && actorId <= 0
                || action != null && (action.isBlank() || action.length() > 100)
                || targetEntity != null && (targetEntity.isBlank() || targetEntity.length() > 100))
            throw new BusinessException("INVALID_AUDIT_FILTER: Use a valid date range, page >= 0, size 1..100 and exact filters");
        Specification<SystemLog> scope = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("category"), admin ? "SYSTEM_SECURITY" : "BUSINESS"));
            predicates.add(cb.isNotNull(root.get("scopeSource")));
            if (!admin) predicates.add(cb.equal(root.get("centerId"), actor.getCenterId()));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (targetEntity != null) predicates.add(cb.equal(root.get("targetEntity"), targetEntity));
            if (actorId != null) predicates.add(cb.equal(root.get("userId"), actorId));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return logs.findAll(scope, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }
}
