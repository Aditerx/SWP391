package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final ClassEnrollmentRepository enrollmentRepository;
    private final SportsClassRepository classRepository;
    private final UserRepository userRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAll(Integer classId, Integer memberId, String status, String actorEmail) {
        User actor = requireActor(actorEmail);
        if (isMemberActor(actor)) {
            memberId = resolveMemberScope(actor, memberId);
        }
        return enrollmentRepository.findFiltered(classId, memberId, status)
                .stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByClassId(Integer classId) {
        return enrollmentRepository.findByClassIdWithDetails(classId)
                .stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByMemberId(Integer memberId, String actorEmail) {
        memberId = resolveMemberScope(requireActor(actorEmail), memberId);
        return enrollmentRepository.findByMemberIdWithDetails(memberId)
                .stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional
    public EnrollmentResponse enroll(Integer classId, Integer targetMemberId, String actorEmail) {
        User actor = requireActor(actorEmail);
        final Integer memberId = resolveMemberScope(actor, targetMemberId);
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + memberId));

        if (!"Active".equalsIgnoreCase(member.getStatus())) {
            throw new BusinessException("Member account is not Active (status: " + member.getStatus() + ")");
        }

        // Validate that member has an active membership package
        List<MemberPackage> activePackages = memberPackageRepository.findByMemberId(memberId).stream()
                .filter(p -> "Active".equalsIgnoreCase(p.getStatus())
                        && p.getEndDate() != null
                        && !p.getEndDate().isBefore(LocalDate.now()))
                .toList();

        if (activePackages.isEmpty()) {
            throw new BusinessException("Member does not have an active membership package or their subscription has expired. Please subscribe or renew first.");
        }

        // Validate class
        SportsClass sportsClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        if ("Closed".equalsIgnoreCase(sportsClass.getStatus()) || "Cancelled".equalsIgnoreCase(sportsClass.getStatus())) {
            throw new BusinessException("Cannot enroll in a class with status '" + sportsClass.getStatus() + "'");
        }

        // Validate capacity
        long currentEnrolled = enrollmentRepository.countBySportsClassIdAndStatus(classId, "Registered");
        if (sportsClass.getMaxCapacity() != null && currentEnrolled >= sportsClass.getMaxCapacity()) {
            throw new BusinessException("Class is full. Capacity is " + sportsClass.getMaxCapacity() + " and already has " + currentEnrolled + " enrolled members.");
        }

        // Check existing enrollment
        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(memberId, classId).orElse(null);
        if (enrollment != null) {
            if ("Registered".equalsIgnoreCase(enrollment.getStatus())) {
                throw new BusinessException("Member is already enrolled in this class");
            }
            // Reactivate cancelled enrollment
            enrollment.setStatus("Registered");
            enrollment.setEnrolledAt(LocalDateTime.now());
        } else {
            enrollment = new ClassEnrollment();
            enrollment.setMember(member);
            enrollment.setSportsClass(sportsClass);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setStatus("Registered");
        }

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        auditService.log(actor.getId(), "ENROLL_CLASS", "CLASS_ENROLLMENT", saved.getId(),
                "Member " + member.getFullName() + " (ID: " + memberId + ") enrolled in class '" + sportsClass.getName() + "' (ID: " + classId + ")");

        return EnrollmentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(Integer classId, Integer targetMemberId, String actorEmail) {
        User actor = requireActor(actorEmail);
        final Integer memberId = resolveMemberScope(actor, targetMemberId);
        ClassEnrollment enrollment = enrollmentRepository.findByMemberIdAndSportsClassId(memberId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("No enrollment found for member " + memberId + " in class " + classId));

        if ("Cancelled".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException("Enrollment is already cancelled");
        }

        enrollment.setStatus("Cancelled");
        ClassEnrollment saved = enrollmentRepository.save(enrollment);

        auditService.log(actor.getId(), "CANCEL_ENROLLMENT", "CLASS_ENROLLMENT", saved.getId(),
                "Member ID " + memberId + " cancelled enrollment in class ID " + classId);

        return EnrollmentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse cancelById(Integer enrollmentId, String actorEmail) {
        User actor = requireActor(actorEmail);
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        resolveMemberScope(actor, enrollment.getMember() != null ? enrollment.getMember().getId() : null);

        enrollment.setStatus("Cancelled");
        ClassEnrollment saved = enrollmentRepository.save(enrollment);

        auditService.log(actor.getId(), "CANCEL_ENROLLMENT", "CLASS_ENROLLMENT", saved.getId(),
                "Cancelled enrollment ID " + enrollmentId);

        return EnrollmentResponse.from(saved);
    }

    private User requireActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
    }

    private Integer resolveMemberScope(User actor, Integer requestedMemberId) {
        if (isMemberActor(actor)) {
            if (requestedMemberId != null && !requestedMemberId.equals(actor.getId())) {
                throw new AccessDeniedException("Members may only access their own enrollments");
            }
            return actor.getId();
        }
        if (requestedMemberId == null) {
            throw new BusinessException("Member ID must be specified");
        }
        return requestedMemberId;
    }

    private boolean isMemberActor(User actor) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean memberAuthority = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()));
        return memberAuthority || (actor.getRole() != null && "Member".equalsIgnoreCase(actor.getRole().getName()));
    }
}
