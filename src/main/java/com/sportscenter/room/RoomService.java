package com.sportscenter.room;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.AccountScope;
import com.sportscenter.user.UserRepository;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final UserRepository users;
    private final RoomRepository repository;
    private final AuditService auditService;
    private final java.time.Clock clock;
    private final com.sportscenter.session.ScheduleWriteLock scheduleLock;

    @Transactional(readOnly = true)
    public List<RoomResponse> findAll() {
        var actor = AccountScope.actor(users);
        return repository.findAll().stream()
                .filter(r -> AccountScope.visible(actor, r.getCenterId()))
                .filter(r -> !("Closed".equals(r.getStatus()) && r.getRoomCode()!=null && r.getRoomCode().contains("-LEGACY-")))
                .map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Room room = new Room();
        var actor = administrationActor();
        room.setCenterId(actor.getCenterId() == null ? 1 : actor.getCenterId());
        apply(room, request);
        Room saved = repository.save(room);
        auditService.log(actor.getId(), "CREATE_ROOM", "ROOM", saved.getId(), configuration(saved));
        return RoomResponse.from(saved);
    }

    @Transactional
    public RoomResponse update(Integer id, RoomRequest request) {
        Room room = getLockedEntity(id);
        String before = configuration(room);
        validateRoomCanBecomeUnavailable(room, request.status());
        if ((!java.util.Objects.equals(room.getCapacity(), request.capacity())
                || request.facilityType() != null && !request.facilityType().equals(room.getFacilityType())
                || request.activityType() != null && !java.util.Objects.equals(RoomActivity.parse(request.activityType()), room.getActivityType()))
                && !repository.findFutureScheduledSessions(id, java.time.LocalDate.now(clock)).isEmpty()) {
            throw new BusinessException("ROOM_HAS_SCHEDULE: Resolve future sessions before changing room capacity, facility or activity type");
        }
        apply(room, request);
        Room saved = repository.save(room);
        if (!before.equals(configuration(saved)))
            auditService.log(administrationActor().getId(), "UPDATE_ROOM", "ROOM", saved.getId(), before + " -> " + configuration(saved));
        return RoomResponse.from(saved);
    }

    @Transactional
    public RoomResponse updateStatus(Integer id, String status) {
        Room room = getLockedEntity(id);
        String previousStatus = room.getStatus();
        validateRoomCanBecomeUnavailable(room, status);
        room.setStatus(normalizeRoomStatus(status));
        Room saved = repository.save(room);
        if (!previousStatus.equalsIgnoreCase(saved.getStatus())) {
            auditService.log(AccountScope.actor(users).getId(), "CHANGE_ROOM_STATUS", "ROOM", saved.getId(),
                    previousStatus + " -> " + saved.getStatus());
        }
        return RoomResponse.from(saved);
    }

    private Room getLockedEntity(Integer id) {
        var actor = administrationActor();
        Integer center = repository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        AccountScope.require(actor, center);
        scheduleLock.acquireForResourceAdministration(center);
        Room room = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        AccountScope.require(AccountScope.actor(users), room.getCenterId());
        return room;
    }

    private com.sportscenter.user.User administrationActor() {
        var actor = AccountScope.actor(users);
        com.sportscenter.common.ActorPolicy.manager(actor);
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().noneMatch(a -> "MANAGE_CLASSES".equals(a.getAuthority())))
            throw new org.springframework.security.access.AccessDeniedException("MANAGE_CLASSES required");
        return actor;
    }

    private String configuration(Room room) {
        return "code=" + room.getRoomCode() + "; name=" + room.getName() + "; location=" + room.getLocation()
                + "; capacity=" + room.getCapacity() + "; facility=" + room.getFacilityType()
                + "; activity=" + room.getActivityType() + "; status=" + room.getStatus();
    }

    private void apply(Room room, RoomRequest request) {
        if (request.roomCode() != null) {
            if (!request.roomCode().matches("C%02d-[A-Z0-9]+-[A-Z0-9]+".formatted(room.getCenterId()))) {
                throw new BusinessException("ROOM_CODE_CENTER_MISMATCH: Room code must match the room center");
            }
            room.setRoomCode(request.roomCode());
        }
        room.setName(request.name());
        room.setLocation(request.location());
        room.setCapacity(request.capacity());
        String status = request.status() == null ? "Available" : normalizeRoomStatus(request.status());
        room.setStatus(status);
        if (request.facilityType() != null && !request.facilityType().isBlank()) {
            room.setFacilityType(request.facilityType());
        }
        if (request.activityType() != null) room.setActivityType(RoomActivity.parse(request.activityType()));
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

        List<RoomScheduleConflict> conflicts = repository.findFutureScheduledSessions(room.getId(), java.time.LocalDate.now(clock));
        if (!conflicts.isEmpty()) {
            String affected = conflicts.stream()
                    .limit(5)
                    .map(item -> "#" + item.getSessionId() + " " + item.getClassName() + " ("
                            + item.getSessionDate() + " " + item.getStartTime() + ")")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            String suffix = conflicts.size() > 5 ? ", ..." : "";
            throw new BusinessException("ROOM_HAS_FUTURE_SESSIONS: Room still has future scheduled sessions: " + affected + suffix
                    + ". Reassign or cancel them before changing the room status.");
        }
    }

}
