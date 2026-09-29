package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.dto.StaffRequest;
import com.sportscenter.user.dto.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CoachRepository coachRepository;
    private final ReceptionistRepository receptionistRepository;
    private final SportsClassRepository sportsClassRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponse> findAllStaff() {
        List<User> users = userRepository.findAll();
        Map<Integer, Coach> coachMap = coachRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(Coach::getUserId, c -> c));

        return users.stream()
                .filter(u -> u.getRole() != null &&
                        ("Coach".equalsIgnoreCase(u.getRole().getName()) ||
                         "Receptionist".equalsIgnoreCase(u.getRole().getName()) ||
                         "CenterManager".equalsIgnoreCase(u.getRole().getName())))
                .map(u -> {
                    Coach coach = coachMap.get(u.getId());
                    int activeClassesCount = "Coach".equalsIgnoreCase(u.getRole().getName())
                            ? (int) sportsClassRepository.findAll().stream()
                                    .filter(c -> c.getCoach() != null && u.getId().equals(c.getCoach().getId()))
                                    .count()
                            : 0;
                    return StaffResponse.fromUser(u, coach, activeClassesCount);
                })
                .toList();
    }

    @Transactional
    public StaffResponse createStaff(StaffRequest request) {
        if (userRepository.findForAuthentication(request.email()).isPresent()) {
            throw new BusinessException("Email already exists: " + request.email());
        }

        String roleName = normalizeRoleName(request.role());
        Role role = roleRepository.findAll().stream()
                .filter(r -> roleName.equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Role not found: " + request.role()));

        User user = new User();
        user.setFullName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(role);
        String rawPassword = (request.password() != null && !request.password().isBlank()) ? request.password() : "12345678";
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(request.status() != null ? normalizeUserStatus(request.status()) : "Active");

        User savedUser = userRepository.save(user);

        Coach savedCoach = null;
        if ("Coach".equalsIgnoreCase(roleName)) {
            Coach coach = new Coach();
            coach.setUser(savedUser);
            coach.setSpecialization(request.specialization());
            savedCoach = coachRepository.save(coach);
        } else if ("Receptionist".equalsIgnoreCase(roleName)) {
            Receptionist receptionist = new Receptionist();
            receptionist.setUser(savedUser);
            receptionistRepository.save(receptionist);
        }

        auditService.log(null, "CREATE_STAFF", "USER", savedUser.getId(), savedUser.getFullName() + " (" + roleName + ")");
        return StaffResponse.fromUser(savedUser, savedCoach, 0);
    }

    @Transactional
    public StaffResponse updateStaff(Integer id, StaffRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + id));

        user.setFullName(request.name());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.status() != null) user.setStatus(normalizeUserStatus(request.status()));

        Coach coach = null;
        if (user.getRole() != null && "Coach".equalsIgnoreCase(user.getRole().getName())) {
            coach = coachRepository.findById(id).orElse(null);
            if (coach != null && request.specialization() != null) {
                coach.setSpecialization(request.specialization());
                coach = coachRepository.save(coach);
            }
        }

        User savedUser = userRepository.save(user);
        auditService.log(null, "UPDATE_STAFF", "USER", savedUser.getId(), savedUser.getFullName());
        return StaffResponse.fromUser(savedUser, coach, 0);
    }

    @Transactional
    public StaffResponse updateStaffStatus(Integer id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + id));
        String previousStatus = user.getStatus();
        String newStatus = normalizeUserStatus(status);
        user.setStatus(newStatus);

        User savedUser = userRepository.save(user);
        Coach coach = coachRepository.findById(id).orElse(null);
        auditService.log(null, "CHANGE_STAFF_STATUS", "USER", savedUser.getId(), previousStatus + " -> " + newStatus);
        return StaffResponse.fromUser(savedUser, coach, 0);
    }

    private String normalizeRoleName(String role) {
        if ("manager".equalsIgnoreCase(role) || "centermanager".equalsIgnoreCase(role)) return "CenterManager";
        if ("coach".equalsIgnoreCase(role)) return "Coach";
        if ("receptionist".equalsIgnoreCase(role)) return "Receptionist";
        return role;
    }

    private String normalizeUserStatus(String status) {
        if ("suspended".equalsIgnoreCase(status) || "locked".equalsIgnoreCase(status)) return "Locked";
        if ("inactive".equalsIgnoreCase(status)) return "Inactive";
        return "Active";
    }
}
