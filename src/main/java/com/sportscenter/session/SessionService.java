package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.holiday.Holiday;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.notification.NotificationTemplates;
import com.sportscenter.notification.NotificationType;
import com.sportscenter.room.Room;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SportsClassRepository sportsClassRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AuditService auditService;
    private final SessionSlotValidator sessionSlotValidator;
    private final HolidayRepository holidayRepository;
    private final Clock clock;
    private final MemberPackageRepository memberPackageRepository;
    private final SubjectRepository subjectRepository;
    private final ScheduleWriteLock scheduleWriteLock;
    private final com.sportscenter.user.MemberRepository memberRepository;
    private final com.sportscenter.specialization.CoachSpecializationRepository coachSpecializationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SessionResponse> findAll(Integer classId, Integer coachId, Integer roomId, Integer memberId,
                                         LocalDate startDate, LocalDate endDate, String status, Authentication authentication) {
        User actor = requireActor(authentication);
        if (hasRole(authentication, "MEMBER")) {
            if (memberId != null && !memberId.equals(actor.getId())) throw new AccessDeniedException("Members may only view their own schedule");
            memberId = actor.getId();
        } else if (hasRole(authentication, "COACH")) {
            if (coachId != null && !coachId.equals(actor.getId())) throw new AccessDeniedException("Coaches may only view their assigned schedule");
            coachId = actor.getId();
        } else {
            requireScheduleStaff(authentication);
        }
        List<Session> list;
        if (memberId != null) {
            list = sessionRepository.findByEnrolledMember(memberId, startDate, endDate, status);
        } else {
            list = sessionRepository.findFiltered(classId, coachId, roomId, startDate, endDate, status);
        }
        var counts = classEnrollmentRepository.countGroupedByClass(List.of("Registered")).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).intValue()));
        final Integer effectiveClass = classId, effectiveCoach = coachId, effectiveRoom = roomId;
        return list.stream().filter(s -> inCenter(actor, s))
                .filter(s -> effectiveClass == null || effectiveClass.equals(s.getSportsClass().getId()))
                .filter(s -> effectiveRoom == null || effectiveRoom.equals(s.getRoom().getId()))
                .filter(s -> effectiveCoach == null || effectiveCoach.equals(coachId(s)))
                .map(s -> {
            int count = s.getBookedMember() != null ? ("Cancelled".equals(s.getStatus()) ? 0 : 1) : counts.getOrDefault(s.getSportsClass().getId(), 0);
            return SessionResponse.from(s, count);
        }).toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse findById(Integer id, Authentication authentication) {
        Session session = sessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        User actor = requireActor(authentication);
        if (!inCenter(actor, session)) throw new AccessDeniedException("Session belongs to another center");
        if (hasRole(authentication, "MEMBER")) {
            boolean owns = session.getBookedMember() != null ? actor.getId().equals(session.getBookedMember().getId())
                    : classEnrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(actor.getId(), session.getSportsClass().getId(), List.of("Registered", "Completed"));
            if (!owns) throw new AccessDeniedException("Members may only view their own sessions");
        } else if (hasRole(authentication, "COACH")) {
            if (!actor.getId().equals(coachId(session))) throw new AccessDeniedException("Session is not assigned to this coach");
        } else requireScheduleStaff(authentication);
        int count = session.getBookedMember() != null ? ("Cancelled".equals(session.getStatus()) ? 0 : 1)
                : (int) classEnrollmentRepository.countBySportsClassIdAndStatus(session.getSportsClass().getId(), "Registered");
        return SessionResponse.from(session, count);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getTimeSlots(Boolean isOutdoor) {
        boolean outdoor = Boolean.TRUE.equals(isOutdoor);
        return TimeSlot.getAvailableSlots(outdoor).stream()
                .map(TimeSlotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogSessionResponse> publicTimetable(Integer classId) {
        SportsClass c = sportsClassRepository.findById(classId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!c.isNormal() || !List.of("Open","Ongoing").contains(c.getStatus())) throw new ResourceNotFoundException("Published Normal class not found");
        return sessionRepository.findFiltered(classId,null,null,null,null,"Scheduled").stream()
                .filter(s -> s.getBookedMember() == null).map(CatalogSessionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CoachAvailableSlotResponse> getCoachAvailableSlots(
            Integer coachId, LocalDate date, Integer subjectId, Integer roomId) {
        if (date == null) {
            throw new BusinessException("Date is required to check coach availability");
        }
        User availableCoach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + coachId));
        if (!"Active".equalsIgnoreCase(availableCoach.getStatus()) || availableCoach.getRole() == null
                || !"Coach".equalsIgnoreCase(availableCoach.getRole().getName())) throw new BusinessException("COACH_INACTIVE: An active coach is required");

        boolean isOutdoor = false;
        if (subjectId != null) {
            Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            if (subject != null) {
                isOutdoor = subject.isOutdoor();
            }
        }
        if (roomId != null) {
            Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            if (room != null) {
                isOutdoor = isOutdoor || room.isOutdoor();
            }
        }

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        if (date.isBefore(today)) {
            return java.util.Arrays.stream(TimeSlot.values())
                    .map(slot -> new CoachAvailableSlotResponse(
                            slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                            false, "Ngày trong quá khứ"))
                    .toList();
        }

        var holidayOpt = holidayRepository.findByHolidayDate(date);
        if (holidayOpt.isPresent() && holidayOpt.get().isActive()) {
            String holidayName = holidayOpt.get().getHolidayName();
            return java.util.Arrays.stream(TimeSlot.values())
                    .map(slot -> new CoachAvailableSlotResponse(
                            slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                            false, "Trung tâm nghỉ lễ: " + holidayName))
                    .toList();
        }

        final boolean outdoor = isOutdoor;
        List<CoachAvailableSlotResponse> responses = new ArrayList<>();

        for (TimeSlot slot : TimeSlot.values()) {
            LocalDateTime slotStart = LocalDateTime.of(date, slot.getStartTime());
            if (slotStart.isBefore(now.plusHours(12))) {
                responses.add(new CoachAvailableSlotResponse(
                    slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                    false, "Yêu cầu đặt trước tối thiểu 12 tiếng"));
            } else if (outdoor && !slot.isOutdoorAllowed()) {
                responses.add(new CoachAvailableSlotResponse(
                    slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                    false, String.format("Ca học bị khóa cho môn thể thao ngoài trời (chính sách chống sốc nhiệt %s - %s)",
                            slot.getStartTime(), slot.getEndTime())));
            } else {
                List<Session> coachConflicts = sessionRepository.findCoachConflicts(
                        coachId, date, slot.getStartTime(), slot.getEndTime(), null);
                if (!coachConflicts.isEmpty()) {
                    responses.add(new CoachAvailableSlotResponse(
                            slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                            false, "HLV đã có lịch dạy vào khung giờ này"));
                } else if (roomId != null) {
                    List<Session> roomConflicts = sessionRepository.findRoomConflicts(
                            roomId, date, slot.getStartTime(), slot.getEndTime(), null);
                    if (!roomConflicts.isEmpty()) {
                        responses.add(new CoachAvailableSlotResponse(
                                slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                                false, "Phòng/sân đã có lịch vào khung giờ này"));
                    } else {
                        responses.add(new CoachAvailableSlotResponse(
                                slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                                true, null));
                    }
                } else {
                    responses.add(new CoachAvailableSlotResponse(
                            slot.getSlotNumber(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                            true, null));
                }
            }
        }
        return responses;
    }

    @Transactional
    public SessionResponse bookPrivateSession(PrivateBookingRequest request, Authentication authentication) {
        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        // All writers take the center lock before subscription/session locks.
        Integer centerId = memberPackageRepository.findCenterIdById(request.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription center not found"));
        scheduleWriteLock.acquire(centerId);
        MemberPackage subscription = memberPackageRepository.findByIdForUpdate(request.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Gói hội viên không tồn tại: " + request.subscriptionId()));

        boolean isMemberRole = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()))
                || (currentUser.getRole() != null && "Member".equalsIgnoreCase(currentUser.getRole().getName()));

        if (isMemberRole && !subscription.getMember().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Hội viên chỉ được đặt lịch bằng gói hội viên của chính mình");
        }

        if (!"Active".equalsIgnoreCase(subscription.getStatus())) {
            throw new BusinessException("Gói hội viên chưa được kích hoạt hoặc đã hết hiệu lực.");
        }

        if (subscription.getMembershipPackage() == null || !subscription.getMembershipPackage().isAdvanced()) {
            throw new BusinessException("Chỉ có thể đặt lịch 1:1 bằng gói hội viên Advanced (12 credit).");
        }

        if (subscription.getRemainingCredit() == null || subscription.getRemainingCredit() < 1) {
            throw new BusinessException("OUT_OF_CREDIT: Gói học 1:1 đã hết tín chỉ (Credit = 0). Vui lòng mua thêm gói.");
        }

        LocalDate today = LocalDate.now(clock);
        if (subscription.getEndDate() != null && today.isAfter(subscription.getEndDate())) {
            throw new BusinessException("ADVANCED_PACKAGE_EXPIRED: Gói học 1:1 đã hết hạn sử dụng 45 ngày.");
        }
        if ((subscription.getStartDate() != null && request.sessionDate().isBefore(subscription.getStartDate()))
                || subscription.getEndDate() == null || request.sessionDate().isAfter(subscription.getEndDate())) {
            throw new BusinessException("ADVANCED_PACKAGE_EXPIRED: Session date must be within the subscription validity period");
        }
        if (!"Active".equalsIgnoreCase(subscription.getMember().getStatus())) {
            throw new BusinessException("MEMBER_NOT_ACTIVE: member account is not active");
        }
        memberRepository.findById(subscription.getMember().getId()).ifPresent(profile -> {
            if (profile.getBookingBlockedUntil() != null && profile.getBookingBlockedUntil().isAfter(LocalDateTime.now(clock))) {
                throw new BusinessException("BOOKING_BLOCKED: member bookings are currently blocked");
            }
        });

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionStart = LocalDateTime.of(request.sessionDate(), request.startTime());
        if (sessionStart.isBefore(now.plusHours(12))) {
            throw new BusinessException("ADVANCED_BOOKING_TOO_LATE: Yêu cầu đặt lịch trước tối thiểu 12 tiếng.");
        }

        if (holidayRepository.existsByHolidayDateAndIsActiveTrue(request.sessionDate())) {
            throw new BusinessException("HOLIDAY_BLOCKED: Không thể đặt lịch vào ngày nghỉ lễ của trung tâm.");
        }

        User coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.coachId()));
        if (coach.getRole() == null || (!"Coach".equalsIgnoreCase(coach.getRole().getName())
                && !"ROLE_COACH".equalsIgnoreCase(coach.getRole().getName()))) {
            throw new BusinessException("Người dùng được chỉ định không phải là Huấn luyện viên.");
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        SportsClass sportsClass;
        if (request.classId() != null) {
            sportsClass = sportsClassRepository.findById(request.classId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
            if (!sportsClass.isAdvanced()) {
                throw new BusinessException("Lớp học đã chọn không phải khóa học 1:1 Advanced.");
            }
        } else if (request.subjectId() != null) {
            sportsClass = sportsClassRepository.findFirstBySubjectIdAndClassType(request.subjectId(), "Advanced")
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học 1:1 mẫu cho bộ môn: " + request.subjectId()));
        } else {
            throw new BusinessException("Cần cung cấp classId hoặc subjectId của ca học 1:1.");
        }

        sessionSlotValidator.validateSession(sportsClass, room, request.startTime(), request.endTime());
        validateRoomAndCapacity(room, sportsClass);
        validateCoach(coach, sportsClass);
        if (!subscription.getMember().getCenterId().equals(sportsClass.getCenterId())
                || !subscription.getMember().getCenterId().equals(room.getCenterId())) {
            throw new AccessDeniedException("Booking resources must belong to the member's center");
        }

        List<Session> coachConflicts = sessionRepository.findCoachConflicts(
                coach.getId(), request.sessionDate(), request.startTime(), request.endTime(), null);
        if (!coachConflicts.isEmpty()) {
            throw new com.sportscenter.common.exception.BusinessConflictException("COACH_CONFLICT: HLV đã có lịch dạy vào khung giờ này.");
        }

        List<Session> roomConflicts = sessionRepository.findRoomConflicts(
                room.getId(), request.sessionDate(), request.startTime(), request.endTime(), null);
        if (!roomConflicts.isEmpty()) {
            throw new com.sportscenter.common.exception.BusinessConflictException("ROOM_CONFLICT: Phòng/sân đã có lịch vào khung giờ này.");
        }

        List<Session> memberConflicts = sessionRepository.findMemberConflicts(
                subscription.getMember().getId(), request.sessionDate(), request.startTime(), request.endTime(), null);
        if (!memberConflicts.isEmpty()) {
            throw new BusinessException("Hội viên đã có lịch học khác trong khung giờ này.");
        }

        // Deduct 1 credit
        subscription.setRemainingCredit(subscription.getRemainingCredit() - 1);
        memberPackageRepository.save(subscription);

        Session session = new Session();
        session.setSportsClass(sportsClass);
        session.setRoom(room);
        session.setCoach(coach);
        session.setBookedMember(subscription.getMember());
        session.setMemberPackage(subscription);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus("Scheduled");
        session.setAttendanceFinalized(false);

        Session saved = sessionRepository.save(session);
        auditService.log(subscription.getMember().getId(), "BOOK_PRIVATE_SESSION", "SESSION", saved.getId(),
                "Booked 1:1 session with coach " + coach.getFullName() + ". Remaining credit: " + subscription.getRemainingCredit());

        publish(NotificationType.PRIVATE_BOOKING_CONFIRMED,
                List.of(subscription.getMember().getId(), coach.getId()),
                NotificationTemplates.privateBookingConfirmed(subjectName(sportsClass), request.sessionDate(),
                        request.startTime(), request.endTime(), coach.getFullName()), "SESSION", saved.getId(),
                "PRIVATE_BOOKING_CONFIRMED:" + saved.getId());
        if (subscription.getRemainingCredit() <= 2) {
            publish(NotificationType.CREDIT_LOW, List.of(subscription.getMember().getId()),
                    NotificationTemplates.creditLow(subscription.getRemainingCredit()),
                    "MEMBER_PACKAGE", subscription.getSubscriptionId(),
                    "CREDIT_LOW:" + subscription.getSubscriptionId() + ":" + saved.getId());
        }

        return SessionResponse.from(saved, 1);
    }

    @Transactional
    public SessionResponse cancelPrivateSession(Integer sessionId, Authentication authentication) {
        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (!session.isPrivateSession() && session.getBookedMember() == null) {
            throw new BusinessException("Chỉ có thể hủy lịch riêng cho ca học 1:1.");
        }

        if ("Cancelled".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Buổi học này đã bị hủy trước đó.");
        }
        if ("Completed".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Buổi học đã hoàn thành, không thể hủy.");
        }

        boolean isMemberRole = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()))
                || (currentUser.getRole() != null && "Member".equalsIgnoreCase(currentUser.getRole().getName()));

        if (isMemberRole && (session.getBookedMember() == null || !session.getBookedMember().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Hội viên chỉ được hủy ca học của chính mình.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        if (sessionStart.isBefore(now.plusHours(24))) {
            throw new BusinessException("CANCEL_TOO_LATE: Không thể hủy lịch khi còn dưới 24 tiếng. Buổi học sẽ bị tiêu thụ nếu không tham gia.");
        }

        session.setStatus("Cancelled");

        // Refund 1 credit
        MemberPackage sub = session.getMemberPackage();
        if (sub != null) {
            sub = memberPackageRepository.findByIdForUpdate(sub.getSubscriptionId()).orElse(null);
        }
        if (sub == null || session.getBookedMember() == null || !sub.getMember().getId().equals(session.getBookedMember().getId())) {
            throw new BusinessException("SUBSCRIPTION_LINK_REQUIRED: Cannot refund credit without the original subscription");
        }
        if (!"Active".equals(sub.getStatus()) || sub.getEndDate() == null || sub.getEndDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("ADVANCED_PACKAGE_EXPIRED: Expired entitlements cannot be refunded as new credit");
        }
        if (sub != null) {
            int limit = sub.getMembershipPackage() != null && sub.getMembershipPackage().getTotalCredit() != null ? sub.getMembershipPackage().getTotalCredit() : 12;
            if (sub.getRemainingCredit() != null && sub.getRemainingCredit() >= limit) throw new BusinessException("CREDIT_LIMIT_EXCEEDED: Credit refund exceeds the original entitlement");
            sub.setRemainingCredit((sub.getRemainingCredit() != null ? sub.getRemainingCredit() : 0) + 1);
            memberPackageRepository.save(sub);
        }

        Session saved = sessionRepository.save(session);
        auditService.log(currentUser.getId(), "CANCEL_PRIVATE_SESSION", "SESSION", saved.getId(),
                "Cancelled 1:1 session >= 24h. Refunded 1 credit. Remaining: " + (sub != null ? sub.getRemainingCredit() : "N/A"));
        List<Integer> recipients = new ArrayList<>();
        if (session.getBookedMember() != null) recipients.add(session.getBookedMember().getId());
        User coach = assignedCoach(session);
        if (coach != null) recipients.add(coach.getId());
        String subject = subjectName(session.getSportsClass());
        publish(NotificationType.PRIVATE_BOOKING_CANCELLED, recipients,
                NotificationTemplates.privateBookingCancelled(subject, session.getSessionDate(), session.getStartTime(),
                        session.getEndTime(), sub != null),
                "SESSION", saved.getId(), "PRIVATE_BOOKING_CANCELLED:" + saved.getId());

        return SessionResponse.from(saved, 0);
    }

    @Transactional
    public SessionResponse create(SessionRequest request) {
        validateDateNotInPast(request.sessionDate());

        scheduleWriteLock.acquire(sportsClassRepository.findCenterIdById(request.classId()).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        sessionSlotValidator.validateSession(sportsClass, room, request.startTime(), request.endTime());
        validateRoomAndCapacity(room, sportsClass);
        if (sportsClass.getCoach() == null) throw new BusinessException("COACH_REQUIRED: Assign a coach before scheduling");
        validateCoach(sportsClass.getCoach(), sportsClass);

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

    @Transactional(readOnly = true)
    public RecurringPreviewResponse previewRecurringSessions(RecurringPreviewRequest request) {
        if (request.startDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("Cannot preview sessions starting in the past");
        }

        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        sessionSlotValidator.validateSession(sportsClass, room, request.startTime(), request.endTime());
        validateRoomAndCapacity(room, sportsClass);

        int targetSessions = sportsClass.getTotalSessions() != null ? sportsClass.getTotalSessions() : 12;
        Map<LocalDate, Holiday> activeHolidays = holidayRepository.findByIsActiveTrue().stream()
                .collect(java.util.stream.Collectors.toMap(Holiday::getHolidayDate, h -> h, (h1, h2) -> h1));

        List<RecurringPreviewResponse.SessionPreviewItem> sessionItems = new ArrayList<>();
        List<RecurringPreviewResponse.SkippedHolidayItem> skippedHolidays = new ArrayList<>();
        boolean globalHasConflict = false;

        LocalDate current = request.startDate();
        LocalDate maxDate = current.plusYears(1);

        while (sessionItems.size() < targetSessions && !current.isAfter(maxDate)) {
            if (request.daysOfWeek().contains(current.getDayOfWeek().getValue())) {
                Holiday holiday = activeHolidays.get(current);
                if (holiday != null) {
                    skippedHolidays.add(new RecurringPreviewResponse.SkippedHolidayItem(
                            current, holiday.getHolidayName(), holiday.getTag()));
                } else {
                    ConflictCheckResponse check = checkScheduleConflict(
                            sportsClass, room, current, request.startTime(), request.endTime(), null);
                    boolean hasConflict = check.hasConflict();
                    if (hasConflict) {
                        globalHasConflict = true;
                    }
                    int sessionNum = sessionItems.size() + 1;
                    sessionItems.add(new RecurringPreviewResponse.SessionPreviewItem(
                            sessionNum,
                            current,
                            current.getDayOfWeek().name(),
                            request.startTime(),
                            request.endTime(),
                            hasConflict,
                            check.conflictDetails()
                    ));
                }
            }
            current = current.plusDays(1);
        }

        if (sessionItems.size() < targetSessions) {
            throw new BusinessException("Không thể tìm đủ " + targetSessions + " buổi học hợp lệ trong vòng 1 năm từ ngày bắt đầu.");
        }

        return new RecurringPreviewResponse(
                sessionItems.size(),
                sportsClass.getId(),
                sportsClass.getName(),
                room.getId(),
                room.getName(),
                globalHasConflict,
                sessionItems,
                skippedHolidays
        );
    }

    @Transactional
    public List<SessionResponse> generateRecurringSessions(SessionGenerateRequest request) {
        if (request.startDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("Cannot generate sessions starting in the past");
        }

        scheduleWriteLock.acquire(sportsClassRepository.findCenterIdById(request.classId()).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        sessionSlotValidator.validateSession(sportsClass, room, request.startTime(), request.endTime());
        validateRoomAndCapacity(room, sportsClass);
        if (sportsClass.getCoach() == null) throw new BusinessException("COACH_REQUIRED: Assign a coach before scheduling");
        validateCoach(sportsClass.getCoach(), sportsClass);

        if (!sportsClass.isNormal()) throw new BusinessException("NORMAL_CLASS_REQUIRED: Recurring generation is only for Normal classes");
        if (sessionRepository.countBySportsClassIdAndStatusNot(sportsClass.getId(), "Cancelled") > 0) throw new BusinessException("SCHEDULE_ALREADY_EXISTS: Recurring schedule has already been generated");
        if (request.daysOfWeek().stream().anyMatch(d -> d == null || d < 1 || d > 7)) throw new BusinessException("INVALID_DAYS: Weekdays must be in 1..7");
        int targetSessions = 12;
        Map<LocalDate, Holiday> activeHolidays = holidayRepository.findByIsActiveTrue().stream()
                .collect(java.util.stream.Collectors.toMap(Holiday::getHolidayDate, h -> h, (h1, h2) -> h1));

        List<Session> toCreate = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        LocalDate current = request.startDate();
        LocalDate maxDate = (request.endDate() != null && request.endDate().isAfter(request.startDate()))
                ? request.endDate().plusYears(1)
                : current.plusYears(1);

        while (toCreate.size() < targetSessions && !current.isAfter(maxDate)) {
            if (request.daysOfWeek().contains(current.getDayOfWeek().getValue())) {
                Holiday holiday = activeHolidays.get(current);
                if (holiday == null) {
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
            }
            current = current.plusDays(1);
        }

        if (!conflicts.isEmpty()) {
            throw new BusinessException("Cannot generate schedule due to conflicts: " + String.join("; ", conflicts));
        }

        if (toCreate.size() < targetSessions) {
            throw new BusinessException("Không thể tìm đủ " + targetSessions + " buổi học hợp lệ: chỉ tìm thấy " + toCreate.size() + " buổi khả dụng.");
        }
        if (!LocalDateTime.of(toCreate.get(0).getSessionDate(), toCreate.get(0).getStartTime()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("CLASS_ALREADY_STARTED: The first session must start in the future");
        }

        LocalDate lastSessionDate = toCreate.get(toCreate.size() - 1).getSessionDate();
        sportsClass.setStartDate(toCreate.get(0).getSessionDate());
        sportsClass.setEndDate(lastSessionDate);
        sportsClassRepository.save(sportsClass);

        List<Session> savedList = sessionRepository.saveAll(toCreate);
        auditService.log(null, "GENERATE_SESSIONS", "SESSION", null,
                "Generated " + savedList.size() + " static recurring sessions for class '" + sportsClass.getName() + "'");

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(sportsClass.getId(), "Registered");
        return savedList.stream().map(s -> SessionResponse.from(s, count)).toList();
    }

    @Transactional
    public SessionResponse update(Integer id, SessionRequest request) {
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        requireMutableSession(session);
        if (session.getBookedMember() != null) throw new BusinessException("USE_INCIDENT_ENDPOINT: Use reschedule for a booked private session");

        SportsClass sportsClass = sportsClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        sessionSlotValidator.validateSession(sportsClass, room, request.startTime(), request.endTime());
        validateRoomAndCapacity(room, sportsClass);
        if (sportsClass.getCoach() == null) throw new BusinessException("COACH_REQUIRED: Assign a coach before scheduling");
        validateCoach(sportsClass.getCoach(), sportsClass);

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
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        if ("Cancelled".equals(status) && (session.getBookedMember() != null
                || classEnrollmentRepository.countBySportsClassIdAndStatusIn(session.getSportsClass().getId(), List.of("Registered", "Pending")) > 0)) {
            throw new BusinessException("INCIDENT_POLICY_REQUIRED: Direct cancellation must not discard a paid entitlement; use reschedule");
        }

        session.setStatus(status);
        Session saved = sessionRepository.save(session);
        auditService.log(null, "UPDATE_SESSION_STATUS", "SESSION", saved.getId(), "Status changed to " + status);

        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(session.getSportsClass().getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    @Transactional
    public void delete(Integer id) {
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
        requireMutableSession(session);
        if (session.getBookedMember() != null || "Open".equals(session.getSportsClass().getStatus())
                || classEnrollmentRepository.countBySportsClassIdAndStatusIn(session.getSportsClass().getId(), List.of("Registered", "Pending")) > 0) {
            throw new BusinessException("SESSION_IN_USE: Published or booked sessions cannot be deleted");
        }
        sessionRepository.delete(session);
        auditService.log(null, "DELETE_SESSION", "SESSION", id, "Deleted session ID " + id);
    }

    @Transactional
    public SessionResponse rescheduleSession(Integer sessionId, SessionRescheduleRequest request, Authentication authentication) {
        Integer actorId = getActorId(authentication);
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if ("Completed".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Không thể dời buổi học đã hoàn thành.");
        }

        requireMutableSession(session);

        if (request.newDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("Không thể dời buổi học về ngày trong quá khứ.");
        }

        if (holidayRepository.existsByHolidayDateAndIsActiveTrue(request.newDate())) {
            throw new BusinessException("Không thể dời buổi học vào ngày nghỉ lễ của trung tâm.");
        }

        Room room = session.getRoom();
        if (request.newRoomId() != null && !request.newRoomId().equals(room.getId())) {
            room = roomRepository.findById(request.newRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.newRoomId()));
        }

        if (!"Available".equalsIgnoreCase(room.getStatus())) {
            throw new BusinessException("Phòng tập mới không sẵn sàng (trạng thái: " + room.getStatus() + ").");
        }

        SportsClass sportsClass = session.getSportsClass();
        validateRoomAndCapacity(room, sportsClass);

        sessionSlotValidator.validateSession(sportsClass, room, request.newStartTime(), request.newEndTime());

        List<Session> roomConflicts = sessionRepository.findRoomConflicts(
                room.getId(), request.newDate(), request.newStartTime(), request.newEndTime(), session.getId());
        if (!roomConflicts.isEmpty()) {
            throw new com.sportscenter.common.exception.BusinessConflictException("ROOM_CONFLICT: Phòng/sân đã có lịch vào khung giờ này.");
        }

        Integer coachId = session.getCoach() != null ? session.getCoach().getId() :
                (sportsClass != null && sportsClass.getCoach() != null ? sportsClass.getCoach().getId() : null);
        if (coachId != null) {
            List<Session> coachConflicts = sessionRepository.findCoachConflicts(
                    coachId, request.newDate(), request.newStartTime(), request.newEndTime(), session.getId());
            if (!coachConflicts.isEmpty()) {
                throw new com.sportscenter.common.exception.BusinessConflictException("COACH_CONFLICT: HLV đã có lịch dạy vào khung giờ này.");
            }
        }

        if (session.isPrivateSession() && session.getBookedMember() != null) {
            List<Session> memberConflicts = sessionRepository.findMemberConflicts(
                    session.getBookedMember().getId(), request.newDate(), request.newStartTime(), request.newEndTime(), session.getId());
            if (!memberConflicts.isEmpty()) {
                throw new BusinessException("Hội viên đã có lịch học khác trong khung giờ này.");
            }
        }

        // Xử lý gói học 1:1 nếu dời vượt quá hạn 45 ngày hoặc có yêu cầu gia hạn
        if (session.isPrivateSession() && session.getMemberPackage() != null) {
            MemberPackage sub = memberPackageRepository.findByIdForUpdate(session.getMemberPackage().getSubscriptionId())
                    .orElse(session.getMemberPackage());
            LocalDate currentEnd = sub.getEndDate() != null ? sub.getEndDate() : LocalDate.now(clock);
            LocalDate targetEnd = currentEnd;
            boolean needExtend = false;

            if (request.newDate().isAfter(currentEnd)) {
                targetEnd = request.newDate();
                needExtend = true;
            }

            if (request.extendValidityDays() != null && request.extendValidityDays() > 0) {
                targetEnd = targetEnd.plusDays(request.extendValidityDays());
                needExtend = true;
            }

            if (needExtend) {
                long daysAdded = java.time.temporal.ChronoUnit.DAYS.between(currentEnd, targetEnd);
                int oldValidity = sub.getValidityDays() != null ? sub.getValidityDays() : 45;
                sub.setValidityDays(oldValidity + (int) daysAdded);
                sub.setEndDate(targetEnd);
                if ("Expired".equalsIgnoreCase(sub.getStatus())) {
                    sub.setStatus("Active");
                }
                memberPackageRepository.save(sub);
                auditService.log(actorId, "INCIDENT_EXTEND_PACKAGE_VALIDITY", "MEMBER_PACKAGE", sub.getSubscriptionId(),
                        String.format("Dời lịch buổi học vượt hạn: Tự động gia hạn gói đến %s (+%d ngày). Lý do sự cố: %s",
                                targetEnd, daysAdded, request.reason() != null ? request.reason() : "N/A"));
            }
        }

        // Xử lý kéo dài ngày kết thúc lớp học Normal nếu dời buổi học vượt quá endDate của lớp
        if (sportsClass != null && sportsClass.getEndDate() != null && request.newDate().isAfter(sportsClass.getEndDate())) {
            sportsClass.setEndDate(request.newDate());
            sportsClassRepository.save(sportsClass);
        }

        LocalDate oldDate = session.getSessionDate();
        LocalTime oldStart = session.getStartTime();
        LocalTime oldEnd = session.getEndTime();
        Room oldRoom = session.getRoom();

        session.setSessionDate(request.newDate());
        session.setStartTime(request.newStartTime());
        session.setEndTime(request.newEndTime());
        session.setRoom(room);
        session.setStatus("Scheduled");
        Session saved = sessionRepository.save(session);

        auditService.log(actorId, "INCIDENT_RESCHEDULE_SESSION", "SESSION", saved.getId(),
                String.format("Dời lịch buổi học #%d từ %s %s-%s (Phòng %s) sang %s %s-%s (Phòng %s). Lý do sự cố: %s",
                        saved.getId(), oldDate, oldStart, oldEnd, oldRoom.getName(),
                        request.newDate(), request.newStartTime(), request.newEndTime(), room.getName(),
                        request.reason() != null ? request.reason() : "Sự cố trung tâm"));

        List<Integer> recipients = sessionRecipients(saved);
        publish(NotificationType.SESSION_RESCHEDULED, recipients,
                NotificationTemplates.sessionRescheduled(subjectName(sportsClass), request.newDate(),
                        request.newStartTime(), request.newEndTime(), room.getName()),
                "SESSION", saved.getId(), "SESSION_RESCHEDULED:" + saved.getId() + ":" + request.newDate()
                        + ":" + request.newStartTime() + ":" + room.getId());

        int count = session.isPrivateSession() ? 1 :
                (int) classEnrollmentRepository.countBySportsClassIdAndStatus(sportsClass.getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    @Transactional
    public SessionResponse substituteCoach(Integer sessionId, SubstituteCoachRequest request, Authentication authentication) {
        Integer actorId = getActorId(authentication);
        scheduleWriteLock.acquire(sessionRepository.findCenterIdById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session center not found")));
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if ("Completed".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Không thể đổi HLV cho buổi học đã hoàn thành.");
        }
        if ("Cancelled".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("Không thể đổi HLV cho buổi học đã bị hủy.");
        }

        requireMutableSession(session);

        User newCoach = userRepository.findById(request.newCoachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.newCoachId()));
        if (newCoach.getRole() == null || (!"Coach".equalsIgnoreCase(newCoach.getRole().getName())
                && !"ROLE_COACH".equalsIgnoreCase(newCoach.getRole().getName()))) {
            throw new BusinessException("Người dùng được chỉ định không phải là Huấn luyện viên.");
        }

        validateCoach(newCoach, session.getSportsClass());
        List<Session> coachConflicts = sessionRepository.findCoachConflicts(
                newCoach.getId(), session.getSessionDate(), session.getStartTime(), session.getEndTime(), session.getId());
        if (!coachConflicts.isEmpty()) {
            throw new com.sportscenter.common.exception.BusinessConflictException("SUBSTITUTE_COACH_CONFLICT: HLV thay thế đã có lịch dạy vào khung giờ này.");
        }

        User oldCoach = session.getCoach() != null ? session.getCoach() :
                (session.getSportsClass() != null ? session.getSportsClass().getCoach() : null);
        session.setCoach(newCoach);
        Session saved = sessionRepository.save(session);

        String oldCoachName = oldCoach != null ? oldCoach.getFullName() : "N/A";
        auditService.log(actorId, "INCIDENT_SUBSTITUTE_COACH", "SESSION", saved.getId(),
                String.format("Thay thế HLV cho buổi học #%d từ %s (ID: %s) sang %s (ID: %d). Lý do sự cố: %s",
                        saved.getId(), oldCoachName, oldCoach != null ? oldCoach.getId() : "null",
                        newCoach.getFullName(), newCoach.getId(),
                        request.reason() != null ? request.reason() : "Sự cố HLV nghỉ"));

        List<Integer> recipients = new ArrayList<>(sessionRecipients(saved));
        if (newCoach.getId() != null) recipients.add(newCoach.getId());
        if (oldCoach != null) recipients.add(oldCoach.getId());
        publish(NotificationType.COACH_SUBSTITUTED, recipients,
                NotificationTemplates.coachSubstituted(subjectName(session.getSportsClass()), session.getSessionDate(),
                        session.getStartTime(), session.getEndTime(), oldCoachName, newCoach.getFullName()),
                "SESSION", saved.getId(), "COACH_SUBSTITUTED:" + saved.getId() + ":" + newCoach.getId());

        int count = session.isPrivateSession() ? 1 :
                (int) classEnrollmentRepository.countBySportsClassIdAndStatus(session.getSportsClass().getId(), "Registered");
        return SessionResponse.from(saved, count);
    }

    private Integer getActorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return null;
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }

    private User requireActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Account not found"));
    }

    private void requireMutableSession(Session session) {
        if (session.isAttendanceFinalized() || "Completed".equals(session.getStatus())
                || !LocalDateTime.of(session.getSessionDate(), session.getEndTime()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("SESSION_HISTORY_IMMUTABLE: Ended or finalized sessions cannot be rescheduled or deleted");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private void requireScheduleStaff(Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> List.of("MANAGE_CLASSES", "REGISTER_MEMBER").contains(a.getAuthority()))) {
            throw new AccessDeniedException("Schedule access requires an assigned operational permission");
        }
    }

    private boolean inCenter(User actor, Session session) {
        return actor.getCenterId() != null && actor.getCenterId().equals(session.getSportsClass().getCenterId());
    }

    private Integer coachId(Session session) {
        return session.getCoach() != null ? session.getCoach().getId()
                : session.getSportsClass().getCoach() != null ? session.getSportsClass().getCoach().getId() : null;
    }

    private void publish(NotificationType type, List<Integer> recipientIds, String message,
                         String relatedType, Integer relatedId, String dedupeKey) {
        eventPublisher.publishEvent(NotificationTemplates.event(type, recipientIds, message, relatedType, relatedId, dedupeKey));
    }

    private List<Integer> sessionRecipients(Session session) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        if (session.getBookedMember() != null) ids.add(session.getBookedMember().getId());
        if (session.getCoach() != null) ids.add(session.getCoach().getId());
        else if (session.getSportsClass() != null && session.getSportsClass().getCoach() != null) {
            ids.add(session.getSportsClass().getCoach().getId());
        }
        if (session.getBookedMember() == null && session.getSportsClass() != null) {
            classEnrollmentRepository.findByClassIdWithDetails(session.getSportsClass().getId()).stream()
                    .filter(enrollment -> "Registered".equalsIgnoreCase(enrollment.getStatus()))
                    .map(enrollment -> enrollment.getMember().getId()).forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private User assignedCoach(Session session) {
        if (session.getCoach() != null) return session.getCoach();
        return session.getSportsClass() == null ? null : session.getSportsClass().getCoach();
    }

    private String subjectName(SportsClass sportsClass) {
        return sportsClass == null || sportsClass.getSubject() == null ? "buổi học" : sportsClass.getSubject().getName();
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
        boolean slotConflict = false;

        var matchedSlot = TimeSlot.findByTime(request.startTime(), request.endTime());
        if (matchedSlot.isEmpty()) {
            slotConflict = true;
            details.add(String.format("Khung giờ %s - %s không hợp lệ (không thuộc 7 ca học chuẩn 90 phút/ca).",
                    request.startTime(), request.endTime()));
        } else {
            boolean isOutdoor = sessionSlotValidator.isOutdoor(sportsClass, room);
            if (isOutdoor && !matchedSlot.get().isOutdoorAllowed()) {
                roomConflict = true;
                slotConflict = true;
                details.add(String.format(SessionSlotValidator.OUTDOOR_HEAT_RESTRICTION_MSG,
                        matchedSlot.get().getName(), matchedSlot.get().getStartTime(), matchedSlot.get().getEndTime()));
            }
        }

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

        boolean hasConflict = roomConflict || coachConflict || slotConflict;
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
        if (!java.util.Objects.equals(room.getCenterId(), sportsClass.getCenterId())) throw new AccessDeniedException("Room belongs to another center");
        if (!"Available".equalsIgnoreCase(room.getStatus())) {
            throw new BusinessException("New or updated sessions require an Available room (current status: " + room.getStatus() + ")");
        }
        if (sportsClass.getMaxCapacity() != null && room.getCapacity() != null
                && sportsClass.getMaxCapacity() > room.getCapacity()) {
            throw new BusinessException("Class capacity (" + sportsClass.getMaxCapacity() +
                    ") cannot exceed room capacity (" + room.getCapacity() + ")");
        }
    }

    private void validateCoach(User coach, SportsClass sportsClass) {
        if (!"Active".equalsIgnoreCase(coach.getStatus()) || coach.getRole() == null
                || !"Coach".equalsIgnoreCase(coach.getRole().getName())) {
            throw new BusinessException("COACH_INACTIVE: An active coach is required");
        }
        if (!java.util.Objects.equals(coach.getCenterId(), sportsClass.getCenterId())) throw new BusinessException("COACH_CENTER_MISMATCH: Coach belongs to another center");
        if (sportsClass.getSubject() == null || coachSpecializationRepository.findByCoachUserId(coach.getId()).stream().noneMatch(a ->
                a.getSpecialization() != null && "Active".equalsIgnoreCase(a.getSpecialization().getStatus())
                && a.getSpecialization().getSubject() != null && sportsClass.getSubject().getId().equals(a.getSpecialization().getSubject().getId()))) {
            throw new BusinessException("COACH_SUBJECT_MISMATCH: Coach must have an active specialization linked to the subject");
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
        if (sessionDate != null && sessionDate.isBefore(LocalDate.now(clock))) {
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
