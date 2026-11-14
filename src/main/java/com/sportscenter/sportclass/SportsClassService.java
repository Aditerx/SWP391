package com.sportscenter.sportclass;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.enrollment.EnrollmentService;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportsClassService {
    private final SportsClassRepository repository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AuditService auditService;
    private final com.sportscenter.invoice.InvoiceRepository invoiceRepository;
    private final EnrollmentService enrollmentService;
    private final java.time.Clock clock;
    private final com.sportscenter.session.SessionRepository sessionRepository;
    private final com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    private final com.sportscenter.membership.AdvancedRegistrationRepository advancedRegistrations;

    @Transactional(readOnly = true)
    public List<SportsClassResponse> findAll() {
        var counts = classEnrollmentRepository.countGroupedByClass(List.of("Registered", "Pending")).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).intValue()));
        User reader=reader();
        var advanced=advancedRegistrations.findAll().stream().filter(r->r.getSportsClass()!=null&&visible(reader,r))
                .collect(java.util.stream.Collectors.toMap(r->r.getSportsClass().getId(),r->r));
        return repository.findAll().stream().filter(c->!c.isAdvanced()||advanced.containsKey(c.getId())).map(c -> {
            if(c.isAdvanced())return SportsClassResponse.advanced(c,advanced.get(c.getId()),clock);
            int count = counts.getOrDefault(c.getId(), 0);
            return SportsClassResponse.from(c, count);
        }).toList();
    }

    @Transactional(readOnly = true)
    public SportsClassResponse findById(Integer id) {
        SportsClass entity = getEntity(id);
        if(entity.isAdvanced()) {
            var r=advancedRegistrations.findBySportsClassId(id).filter(x->visible(reader(),x))
                    .orElseThrow(()->new ResourceNotFoundException("Private class not found"));
            return SportsClassResponse.advanced(entity,r,clock);
        }
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(entity.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(entity, count);
    }

    @Transactional
    public SportsClassResponse create(SportsClassRequest request) {
        User actor = publicationActor();
        if("Advanced".equalsIgnoreCase(request.classType()))throw new BusinessException("USE_ADVANCED_REGISTRATION: Coach acceptance creates the private class");
        validateNewClassDate(request);
        validateDateRange(request);
        SportsClass sportsClass = new SportsClass();
        sportsClass.setCenterId(actor.getCenterId());
        apply(sportsClass, request);
        if (sportsClass.isNormal()) sportsClass.setMinCapacity(NormalClassPolicy.validateMinimum(sportsClass.getMaxCapacity(), request.minCapacity()));
        // The legacy two-request setup remains closed until the schedule is complete.
        if (sportsClass.isNormal()) { sportsClass.setStatus("Closed"); sportsClass.setPricingDraft(true); }
        SportsClass saved = repository.save(sportsClass);
        auditService.log(actor.getId(), "CREATE_CLASS", "CLASS", saved.getId(), saved.getName() + "; tuitionFee=" + saved.getTuitionFee());
        return SportsClassResponse.from(saved, 0);
    }

    @Transactional
    public SportsClassResponse update(Integer id, SportsClassRequest request, String actorEmail) {
        User actor = publicationActor();
        validateDateRange(request);
        scheduleWriteLock.acquire(repository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
        if(sportsClass.isAdvanced())throw new BusinessException("ADVANCED_CLASS_WORKFLOW_REQUIRED: Use registration and private-session actions");
        if (request.classType()!=null && !request.classType().equalsIgnoreCase(sportsClass.getClassType()==null?"Normal":sportsClass.getClassType()))
            throw new BusinessException("CLASS_TYPE_IMMUTABLE: Normal and Advanced use separate creation workflows");
        Integer previousCapacity = sportsClass.getMaxCapacity();
        BigDecimal previousFee = sportsClass.getTuitionFee();
        boolean draft = sportsClass.isNormal() && sportsClass.isPricingDraft()
                && !classEnrollmentRepository.existsBySportsClassId(id) && !invoiceRepository.existsBySportsClassId(id);
        if (sportsClass.isNormal()) {
            com.sportscenter.common.ActorPolicy.center(actor, sportsClass.getCenterId());
            String requestedStatus = request.status() == null ? sportsClass.getStatus() : normalizeClassStatus(request.status());
            if (!requestedStatus.equals(sportsClass.getStatus()) && !(draft && "Open".equals(requestedStatus)))
                throw new BusinessException("NORMAL_LIFECYCLE_ACTION_REQUIRED: Use Normal lifecycle actions; status cannot be assigned by PUT");
            if (!draft && request.minCapacity() != null && !java.util.Objects.equals(request.minCapacity(), sportsClass.getMinCapacity()))
                throw new BusinessException("NORMAL_MIN_CAPACITY_LOCKED: Minimum capacity is fixed after creation/publication or any registration");
            if (draft) sportsClass.setMinCapacity(NormalClassPolicy.validateMinimum(request.maxCapacity(), request.minCapacity() == null ? sportsClass.getMinCapacity() : request.minCapacity()));
            else if (sportsClass.getMinCapacity() != null && request.maxCapacity() < sportsClass.getMinCapacity())
                throw new BusinessException("NORMAL_CAPACITY_INVALID: Maximum cannot be below the fixed minimum");
        }
        if (draft && "Ongoing".equalsIgnoreCase(request.status()))
            throw new BusinessException("NORMAL_PUBLICATION_STATUS: Publish a new Normal draft as Open after its schedule is complete");
        if (!draft && sportsClass.isNormal() && request.tuitionFee() != null
                && (previousFee == null || previousFee.compareTo(request.tuitionFee()) != 0))
            throw new com.sportscenter.common.exception.BusinessConflictException("CLASS_REPRICING_NOT_APPROVED: Repricing published or legacy classes requires separate Owner review");
        if (sportsClass.isNormal() && (request.startDate()!=null && !java.util.Objects.equals(request.startDate(), sportsClass.getStartDate())
                || request.endDate()!=null && !java.util.Objects.equals(request.endDate(), sportsClass.getEndDate())))
            throw new BusinessException("NORMAL_SCHEDULE_ACTION_REQUIRED: Class dates follow the persisted schedule");
        long occupied = classEnrollmentRepository.countBySportsClassIdAndStatusIn(id, List.of("Registered", "Pending"));
        if (request.maxCapacity() != null && request.maxCapacity() < occupied) throw new BusinessException("CLASS_CAPACITY_EXCEEDED: Cannot reduce capacity below reserved seats");
        if ((sessionRepository.countBySportsClassIdAndStatusNot(id, "Cancelled") > 0||sessionRepository.existsBySportsClassId(id))
                && (!java.util.Objects.equals(request.subjectId(), sportsClass.getSubject().getId())
                || request.classType() != null && !request.classType().equalsIgnoreCase(sportsClass.getClassType())
                || !java.util.Objects.equals(request.coachId(), sportsClass.getCoach() == null ? null : sportsClass.getCoach().getId()))) {
            throw new BusinessException("CLASS_HAS_SCHEDULE: Use per-session incident handling to change the assigned coach; the subject/type is fixed once scheduled");
        }
        if (sportsClass.isNormal() && "Open".equalsIgnoreCase(request.status())) {
            com.sportscenter.session.NormalSchedulePolicy.requireReady(sportsClass, sessionRepository, clock);
        }
        String previousStatus = sportsClass.getStatus();
        var previousStart = sportsClass.getStartDate(); var previousEnd = sportsClass.getEndDate();
        apply(sportsClass, request);
        if (request.status() == null) sportsClass.setStatus(previousStatus);
        if (sportsClass.isNormal()) {
            if (request.startDate()==null) sportsClass.setStartDate(previousStart);
            if (request.endDate()==null) sportsClass.setEndDate(previousEnd);
        }
        // Omitted prices preserve the draft's chosen price as well as every published price.
        if (previousFee != null && (!draft || request.tuitionFee() == null)) sportsClass.setTuitionFee(previousFee);
        boolean publishedNow = draft && "Open".equals(sportsClass.getStatus());
        sportsClass.setPricingDraft(draft && "Closed".equals(sportsClass.getStatus()));
        SportsClass saved = repository.save(sportsClass);
        if (publishedNow || previousFee == null || previousFee.compareTo(saved.getTuitionFee()) != 0)
            auditService.log(actor.getId(), publishedNow ? "PUBLISH_CLASS" : "SET_CLASS_PRICE", "CLASS", saved.getId(),
                    "tuitionFee " + previousFee + " -> " + saved.getTuitionFee());
        if (saved.getMaxCapacity() != null && (previousCapacity == null || saved.getMaxCapacity() > previousCapacity)) {
            Integer actorId = userRepository.findByEmailIgnoreCase(actorEmail).map(User::getId).orElse(null);
            enrollmentService.promoteWaitlist(saved.getId(), actorId);
        }
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(saved.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(saved, count);
    }

    @Transactional
    public SportsClassResponse assignCoach(Integer id, Integer coachId) {
        scheduleWriteLock.acquire(repository.findCenterIdById(id).orElseThrow(() -> new ResourceNotFoundException("Class center not found")));
        SportsClass sportsClass = repository.findByIdForUpdate(id).orElseThrow();
        if(sportsClass.isAdvanced())throw new BusinessException("ADVANCED_CLASS_WORKFLOW_REQUIRED: Primary Coach follows the approved registration");
        if (sessionRepository.countBySportsClassIdAndStatusNot(id, "Cancelled") > 0||sessionRepository.existsBySportsClassId(id)) throw new BusinessException("CLASS_HAS_SCHEDULE: Use substitute-coach for individual scheduled sessions to preserve history");
        User coach = getCoach(coachId);
        sportsClass.setCoach(coach);
        SportsClass saved = repository.save(sportsClass);
        auditService.log(null, "ASSIGN_COACH", "CLASS", saved.getId(), "coachId=" + coachId);
        int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(saved.getId(), List.of("Registered", "Pending"));
        return SportsClassResponse.from(saved, count);
    }

    private void apply(SportsClass sportsClass, SportsClassRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        if (!"Active".equalsIgnoreCase(subject.getStatus())) throw new BusinessException("SUBJECT_INACTIVE: Cannot create or publish classes for an inactive subject");
        sportsClass.setName(request.name());
        sportsClass.setSubject(subject);
        sportsClass.setCoach(request.coachId() == null ? null : getCoach(request.coachId()));

        String classType = (request.classType() != null && !request.classType().isBlank())
                ? request.classType()
                : (sportsClass.getClassType() != null ? sportsClass.getClassType() : "Normal");

        if ("Normal".equalsIgnoreCase(classType)) {
            if (request.maxCapacity() != null && request.maxCapacity() > 15) {
                throw new BusinessException("Lớp học nhóm Normal chỉ có sức chứa tối đa 15 học viên (maxCapacity <= 15).");
            }
            sportsClass.setClassType("Normal");
            if (request.totalSessions() != null && request.totalSessions() != 12) throw new BusinessException("NORMAL_SESSION_COUNT: Normal classes require exactly 12 sessions");
            sportsClass.setTotalSessions(12);
            BigDecimal price = request.tuitionFee() == null ? subject.getNormalPrice() : request.tuitionFee();
            if (price != null && price.signum() > 0 && price.stripTrailingZeros().scale() <= 2 && price.precision() - price.scale() <= 10) {
                sportsClass.setTuitionFee(price);
            } else {
                throw new BusinessException("INVALID_PRICE: Supply a positive publication price with at most two decimal places, or use a valid Subject default");
            }
        } else if ("Advanced".equalsIgnoreCase(classType)) {
            if (request.maxCapacity() != null && request.maxCapacity() != 1) {
                throw new BusinessException("Khóa học 1:1 Advanced bắt buộc có sức chứa bằng 1 (maxCapacity = 1).");
            }
            sportsClass.setClassType("Advanced");
            sportsClass.setTotalSessions(12);
            if (subject.getAdvancedPrice() != null && subject.getAdvancedPrice().compareTo(BigDecimal.ZERO) > 0) {
                sportsClass.setTuitionFee(subject.getAdvancedPrice());
            } else {
                throw new BusinessException("INVALID_PRICE: A positive server subject price is required; client tuition overrides are unsupported");
            }
        } else {
            throw new BusinessException("Loại lớp học không hợp lệ: " + classType + ". Chỉ chấp nhận 'Normal' hoặc 'Advanced'.");
        }

        sportsClass.setMaxCapacity(request.maxCapacity());
        sportsClass.setStartDate(request.startDate());
        sportsClass.setEndDate(request.endDate());
        sportsClass.setStatus(request.status() == null ? "Open" : normalizeClassStatus(request.status()));
    }

    private String normalizeClassStatus(String status) {
        if ("Open".equalsIgnoreCase(status)) return "Open";
        if ("Ongoing".equalsIgnoreCase(status)) return "Ongoing";
        if ("Closed".equalsIgnoreCase(status)) return "Closed";
        if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
        throw new BusinessException("Invalid class status: " + status + ". Must be Open, Ongoing, Closed, or Cancelled");
    }

    @Transactional
    public void publishCreatedNormal(SportsClass sportsClass) {
        User actor = publicationActor();
        com.sportscenter.common.ActorPolicy.center(actor, sportsClass.getCenterId());
        com.sportscenter.session.NormalSchedulePolicy.requireReady(sportsClass, sessionRepository, clock);
        sportsClass.setStatus("Open"); sportsClass.setPricingDraft(false);
        auditService.log(actor.getId(), "PUBLISH_CLASS", "CLASS", sportsClass.getId(), "tuitionFee=" + sportsClass.getTuitionFee());
    }

    private User publicationActor() {
        User actor = com.sportscenter.common.AccountScope.actor(userRepository);
        com.sportscenter.common.ActorPolicy.manager(actor);
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().noneMatch(a -> "MANAGE_CLASSES".equals(a.getAuthority())))
            throw new org.springframework.security.access.AccessDeniedException("MANAGE_CLASSES required");
        if (actor.getCenterId() == null) throw new BusinessException("CENTER_REQUIRED: Class publication requires a center assignment");
        return actor;
    }

    private SportsClass getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
    }

    private User reader() {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth==null?null:userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }
    private boolean visible(User actor,com.sportscenter.membership.AdvancedRegistration r) {
        if(actor==null||!"Active".equals(actor.getStatus())||!java.util.Objects.equals(actor.getCenterId(),r.getMember().getCenterId()))return false;
        if(com.sportscenter.common.ActorPolicy.role(actor,"Member"))return actor.getId().equals(r.getMember().getId());
        if(com.sportscenter.common.ActorPolicy.role(actor,"Coach"))return actor.getId().equals(r.getCoach().getId());
        return com.sportscenter.common.ActorPolicy.role(actor,"Admin")||com.sportscenter.common.ActorPolicy.role(actor,"CenterManager")
                ||com.sportscenter.common.ActorPolicy.role(actor,"Receptionist");
    }

    private User getCoach(Integer coachId) {
        User user = userRepository.findById(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Coach user not found: " + coachId));
        if (user.getRole() == null || user.getRole().getName() == null
                || !"Coach".equalsIgnoreCase(user.getRole().getName())) {
            throw new BusinessException("User " + coachId + " does not have the Coach role");
        }
        return user;
    }

    private void validateNewClassDate(SportsClassRequest request) {
        if ("Open".equalsIgnoreCase(request.status())
                && request.startDate() != null
                && request.startDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("A new Open class cannot start in the past");
        }
    }

    private void validateDateRange(SportsClassRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Class end date cannot be before its start date");
        }
    }
}
