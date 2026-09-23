package com.sportscenter.room;

import com.sportscenter.audit.AuditService;
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
        apply(room, request);
        return RoomResponse.from(repository.save(room));
    }

    @Transactional
    public RoomResponse updateStatus(Integer id, String status) {
        Room room = getEntity(id);
        room.setStatus(status);
        return RoomResponse.from(repository.save(room));
    }

    private Room getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
    }

    private void apply(Room room, RoomRequest request) {
        room.setName(request.name());
        room.setLocation(request.location());
        room.setCapacity(request.capacity());
        room.setStatus(request.status() == null ? "ACTIVE" : request.status());
    }
}
