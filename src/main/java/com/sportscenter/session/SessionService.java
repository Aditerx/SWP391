package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.room.Room;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SportsClassRepository sportsClassRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SessionResponse> findAll(Integer classId, Integer coachId, Integer roomId, Integer memberId,
                                         LocalDate startDate, LocalDate endDate, String status) {
        List<Session> list;
        if (memberId != null) {
            list = sessionRepository.findByEnrolledMember(memberId, startDate, endDate, status);
        } else {
            list = sessionRepository.findFiltered(classId, coachId, roomId, startDate, endDate, status);
        }
        return list.stream().map(s -> {
            int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(s.getSportsClass().getId(), "Registered");
            return SessionResponse.from(s, count);
        }).toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse findById(Integer id) {
        Session session = sessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(session.getSportsClass().getId(), "Registered");
        return SessionResponse.from(session, count);
    }

    @Transactional
    public SessionResponse create(SessionRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        validateDateNotInPast(request.sessionDate());

        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        validateRoomAndCapacity(room, sportsClass);

        ConflictCheckResponse conflict = checkScheduleConflict(
                sportsClass, room, request.sessionDate(), request.startTime(), request.endTime(), null);
        if (conflict.hasConflict()) {
            throw new BusinessException(conflict.message());
        }

        Session session = new Session();
        session.setSportsClass(sportsClass);
        session.setRoom(room);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus(request.status() == null ? "Scheduled" : normalizeSessionStatus(request.status()));

        Session saved = sessionRepository.save(session);
        auditService.log(null, "CREATE_SESSION", "SESSION", saved.getId(),
                "Created session for class '" + sportsClass.getName() + "' in room '" + room.getName() + "' on " + request.sessionDate());

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(sportsClass.getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    @Transactional
    public List<SessionResponse> generateRecurringSessions(SessionGenerateRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("End date cannot be before start date");
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot generate sessions starting in the past");
        }

        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        validateRoomAndCapacity(room, sportsClass);

        List<DayOfWeek> targetDays = request.daysOfWeek().stream()
                .map(DayOfWeek::of)
                .toList();

        List<Session> toCreate = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        LocalDate current = request.startDate();
        while (!current.isAfter(request.endDate())) {
            if (targetDays.contains(current.getDayOfWeek())) {
                ConflictCheckResponse check = checkScheduleConflict(
                        sportsClass, room, current, request.startTime(), request.endTime(), null);
                if (check.hasConflict()) {
                    conflicts.addAll(check.conflictDetails());
                } else {
                    Session s = new Session();
                    s.setSportsClass(sportsClass);
                    s.setRoom(room);
                    s.setSessionDate(current);
                    s.setStartTime(request.startTime());
                    s.setEndTime(request.endTime());
                    s.setStatus("Scheduled");
                    toCreate.add(s);
                }
            }
            current = current.plusDays(1);
        }

        if (!conflicts.isEmpty()) {
            throw new BusinessException("Cannot generate schedule due to conflicts: " + String.join("; ", conflicts));
        }

        if (toCreate.isEmpty()) {
            throw new BusinessException("No session dates matched the specified days of the week in range");
        }

        List<Session> savedList = sessionRepository.saveAll(toCreate);
        auditService.log(null, "GENERATE_SESSIONS", "SESSION", null,
                "Generated " + savedList.size() + " recurring sessions for class '" + sportsClass.getName() + "'");

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(sportsClass.getId(), "Registered");
        return savedList.stream().map(s -> SessionResponse.from(s, count)).toList();
    }

    @Transactional
    public SessionResponse update(Integer id, SessionRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        Session session = sessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));

        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        validateRoomAndCapacity(room, sportsClass);

        ConflictCheckResponse conflict = checkScheduleConflict(
                sportsClass, room, request.sessionDate(), request.startTime(), request.endTime(), id);
        if (conflict.hasConflict()) {
            throw new BusinessException(conflict.message());
        }

        session.setSportsClass(sportsClass);
        session.setRoom(room);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        if (request.status() != null) {
            session.setStatus(normalizeSessionStatus(request.status()));
        }

        Session saved = sessionRepository.save(session);
        auditService.log(null, "UPDATE_SESSION", "SESSION", saved.getId(),
                "Updated session ID " + id + " to " + request.sessionDate() + " " + request.startTime() + "-" + request.endTime());

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(sportsClass.getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    @Transactional
    public SessionResponse updateStatus(Integer id, String rawStatus) {
        String status = normalizeSessionStatus(rawStatus);
        Session session = sessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));

        session.setStatus(status);
        Session saved = sessionRepository.save(session);
        auditService.log(null, "UPDATE_SESSION_STATUS", "SESSION", saved.getId(), "Status changed to " + status);

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(session.getSportsClass().getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    @Transactional
    public void delete(Integer id) {
        Session session = sessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        sessionRepository.delete(session);
        auditService.log(null, "DELETE_SESSION", "SESSION", id, "Deleted session ID " + id);
    }

    @Transactional(readOnly = true)
    public ConflictCheckResponse checkConflict(ConflictCheckRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        SportsClass sportsClass = null;
        if (request.classId() != null) {
            sportsClass = sportsClassRepository.findById(request.classId()).orElse(null);
        }

        Room room = null;
        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId()).orElse(null);
        }

        Integer coachId = request.coachId();
        if (coachId == null && sportsClass != null && sportsClass.getCoach() != null) {
            coachId = sportsClass.getCoach().getId();
        }

        List<String> details = new ArrayList<>();
        boolean roomConflict = false;
        boolean coachConflict = false;

        if (room != null) {
            if (!"Available".equalsIgnoreCase(room.getStatus())) {
                roomConflict = true;
                details.add("Phòng '" + room.getName() + "' hiện đang ở trạng thái '" + room.getStatus() + "' (không sẵn sàng).");
            }

            List<Session> roomSessions = sessionRepository.findRoomConflicts(
                    room.getId(), request.sessionDate(), request.startTime(), request.endTime(), request.excludeSessionId());
            if (!roomSessions.isEmpty()) {
                roomConflict = true;
                for (Session s : roomSessions) {
                    details.add("Trùng phòng: Phòng '" + room.getName() + "' đã có buổi học của lớp '" +
                            s.getSportsClass().getName() + "' từ " + s.getStartTime() + " đến " + s.getEndTime() + " ngày " + s.getSessionDate());
                }
            }
        }

        if (coachId != null) {
            List<Session> coachSessions = sessionRepository.findCoachConflicts(
                    coachId, request.sessionDate(), request.startTime(), request.endTime(), request.excludeSessionId());
            if (!coachSessions.isEmpty()) {
                coachConflict = true;
                User coach = userRepository.findById(coachId).orElse(null);
                String coachName = coach != null ? coach.getFullName() : "HLV #" + coachId;
                for (Session s : coachSessions) {
                    details.add("Trùng HLV: " + coachName + " đã có lịch dạy lớp '" +
                            s.getSportsClass().getName() + "' (Phòng " + s.getRoom().getName() + ") từ " + s.getStartTime() + " đến " + s.getEndTime() + " ngày " + s.getSessionDate());
                }
            }
        }

        boolean hasConflict = roomConflict || coachConflict;
        String message = hasConflict ? String.join(" | ", details) : "Không có xung đột lịch phòng hoặc HLV.";
        return new ConflictCheckResponse(hasConflict, roomConflict, coachConflict, message, details);
    }

    private ConflictCheckResponse checkScheduleConflict(
            SportsClass sportsClass, Room room, LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Integer excludeId) {
        Integer coachId = (sportsClass != null && sportsClass.getCoach() != null) ? sportsClass.getCoach().getId() : null;
        ConflictCheckRequest req = new ConflictCheckRequest(
                sportsClass != null ? sportsClass.getId() : null,
                coachId,
                room != null ? room.getId() : null,
                sessionDate,
                startTime,
                endTime,
                excludeId
        );
        return checkConflict(req);
    }

    private void validateRoomAndCapacity(Room room, SportsClass sportsClass) {
        if (!"Available".equalsIgnoreCase(room.getStatus())) {
            throw new BusinessException("New or updated sessions require an Available room (current status: " + room.getStatus() + ")");
        }
        if (sportsClass.getMaxCapacity() != null && room.getCapacity() != null
                && sportsClass.getMaxCapacity() > room.getCapacity()) {
            throw new BusinessException("Class capacity (" + sportsClass.getMaxCapacity() +
                    ") cannot exceed room capacity (" + room.getCapacity() + ")");
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("End time must be strictly after start time");
        }
    }

    private void validateDateNotInPast(LocalDate sessionDate) {
        if (sessionDate != null && sessionDate.isBefore(LocalDate.now())) {
            throw new BusinessException("A new session cannot be created in the past");
        }
    }

    private String normalizeSessionStatus(String status) {
        if ("Scheduled".equalsIgnoreCase(status)) return "Scheduled";
        if ("Completed".equalsIgnoreCase(status)) return "Completed";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        throw new BusinessException("Invalid session status: " + status + ". Must be Scheduled, Completed, or Cancelled");
    }
}
