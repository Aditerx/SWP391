package com.sportscenter.room;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RoomResponse> findAll() {
        return repository.findAll().stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Room room = new Room();
        apply(room, request);
        Room saved = repository.save(room);
        auditService.log(null, "CREATE_ROOM", "ROOM", saved.getId(), saved.getName());
        return RoomResponse.from(saved);
    }

    @Transactional
    public RoomResponse update(Integer id, RoomRequest request) {
        Room room = getEntity(id);
        String previousStatus = room.getStatus();
        validateRoomCanBecomeUnavailable(room, request.status());
        apply(room, request);
        Room saved = repository.save(room);
        auditStatusChange(previousStatus, saved, "UPDATE_ROOM");
        return RoomResponse.from(saved);
    }

    @Transactional
    public RoomResponse updateStatus(Integer id, String status) {
        Room room = getEntity(id);
        String previousStatus = room.getStatus();
        validateRoomCanBecomeUnavailable(room, status);
        room.setStatus(status);
        Room saved = repository.save(room);
        if (!previousStatus.equalsIgnoreCase(saved.getStatus())) {
            auditService.log(null, "CHANGE_ROOM_STATUS", "ROOM", saved.getId(),
                    previousStatus + " -> " + saved.getStatus());
        }
        return RoomResponse.from(saved);
    }

    private Room getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
    }

    private void apply(Room room, RoomRequest request) {
        room.setName(request.name());
        room.setLocation(request.location());
        room.setCapacity(request.capacity());
        String status = request.status() == null ? "Available" : normalizeRoomStatus(request.status());
        room.setStatus(status);
    }

    private String normalizeRoomStatus(String status) {
        if ("Available".equalsIgnoreCase(status)) return "Available";
        if ("Maintenance".equalsIgnoreCase(status)) return "Maintenance";
        if ("Closed".equalsIgnoreCase(status)) return "Closed";
        throw new BusinessException("Invalid room status: " + status + ". Must be Available, Maintenance, or Closed");
    }

    private void validateRoomCanBecomeUnavailable(Room room, String requestedStatus) {
        if (requestedStatus == null || "Available".equalsIgnoreCase(requestedStatus)
                || requestedStatus.equalsIgnoreCase(room.getStatus())) {
            return;
        }

        List<RoomScheduleConflict> conflicts = repository.findFutureScheduledSessions(room.getId());
        if (!conflicts.isEmpty()) {
            String affected = conflicts.stream()
                    .limit(5)
                    .map(item -> "#" + item.getSessionId() + " " + item.getClassName() + " ("
                            + item.getSessionDate() + " " + item.getStartTime() + ")")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            String suffix = conflicts.size() > 5 ? ", ..." : "";
            throw new BusinessException("Room still has future scheduled sessions: " + affected + suffix
                    + ". Reassign or cancel them before changing the room status.");
        }
    }

    private void auditStatusChange(String previousStatus, Room saved, String action) {
        if (previousStatus != null && !previousStatus.equalsIgnoreCase(saved.getStatus())) {
            auditService.log(null, action, "ROOM", saved.getId(),
                    previousStatus + " -> " + saved.getStatus());
        }
    }
}
