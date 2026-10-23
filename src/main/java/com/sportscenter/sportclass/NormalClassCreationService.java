package com.sportscenter.sportclass;

import com.sportscenter.center.CenterContext;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.session.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class NormalClassCreationService {
    private final SportsClassService classService;
    private final SportsClassRepository classRepository;
    private final NormalClassCreationRepository requests;
    private final SessionService sessionService;
    private final ScheduleWriteLock lock;
    private final CenterContext centers;
    private final ObjectMapper objectMapper;

    @Transactional
    public SportsClassResponse create(NormalClassCreationRequest request) {
        Integer centerId = centers.currentCenterId();
        lock.acquire(centerId);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(request))); }
        catch (Exception ex) { throw new IllegalStateException("Could not identify class request", ex); }
        NormalClassCreation previous = requests.findById(request.requestId().toString()).orElse(null);
        if (previous != null) {
            if (!centerId.equals(previous.getCenterId()) || !hash.equals(previous.getRequestHash())) throw new com.sportscenter.common.exception.BusinessConflictException("IDEMPOTENCY_CONFLICT: requestId was already used for another request");
            return classService.findById(previous.getSportsClass().getId());
        }
        var r = request.sportsClass();
        if (r.startDate() == null || r.coachId() == null || (r.classType() != null && !"Normal".equalsIgnoreCase(r.classType()))) {
            throw new BusinessException("NORMAL_CLASS_REQUIRED: A Normal class, coach and startDate are required");
        }
        var setup = new SportsClassRequest(r.name(), r.subjectId(), r.coachId(), r.maxCapacity(), r.tuitionFee(), r.startDate(), r.endDate(), "Closed", "Normal", 12);
        Integer id = classService.create(setup).id();
        SportsClass entity = classRepository.findById(id).orElseThrow();
        entity.setCenterId(centerId);
        sessionService.generateRecurringSessions(new SessionGenerateRequest(id, request.roomId(), r.startDate(), request.daysOfWeek(), request.startTime(), request.endTime()));
        entity.setStatus("Open");
        var saved = new NormalClassCreation();
        saved.setRequestId(request.requestId().toString()); saved.setRequestHash(hash); saved.setCenterId(centerId); saved.setSportsClass(entity);
        requests.save(saved);
        return classService.findById(id);
    }
}
