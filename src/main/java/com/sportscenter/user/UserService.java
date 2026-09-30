package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.dto.UserRequest;
import com.sportscenter.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CoachRepository coachRepository;
    private final ReceptionistRepository receptionistRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers(String roleFilter, String search) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .filter(u -> {
                    if (roleFilter != null && !roleFilter.isBlank() && !"all".equalsIgnoreCase(roleFilter)) {
                        if (u.getRole() == null || !roleFilter.equalsIgnoreCase(u.getRole().getName())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(s);
                        boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(s);
                        boolean matchPhone = u.getPhone() != null && u.getPhone().contains(s);
                        if (!matchName && !matchEmail && !matchPhone) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.findForAuthentication(request.email().trim()).isPresent()) {
            throw new BusinessException("Email already exists: " + request.email());
        }

        Role role = null;
        if (request.roleId() != null) {
            role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.roleId()));
        } else if (request.roleName() != null && !request.roleName().isBlank()) {
            role = roleRepository.findByNameIgnoreCase(request.roleName().trim())
                    .orElseThrow(() -> new BusinessException("Role not found with name: " + request.roleName()));
        } else {
            role = roleRepository.findByNameIgnoreCase("Member")
                    .orElseThrow(() -> new BusinessException("Default Member role not found"));
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole(role);
        String rawPassword = (request.password() != null && !request.password().isBlank()) ? request.password() : "Scms@2026";
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(request.status() != null ? normalizeUserStatus(request.status()) : "Active");

        User savedUser = userRepository.save(user);

        // Sync subtype table if necessary
        syncSubtypeRecord(savedUser, role.getName());

        auditService.log(null, "CREATE_USER", "USER", savedUser.getId(),
                "Created user " + savedUser.getFullName() + " (" + savedUser.getEmail() + ") with role " + role.getName());

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Integer id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.status() != null) {
            user.setStatus(normalizeUserStatus(request.status()));
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.roleId() != null && (user.getRole() == null || !request.roleId().equals(user.getRole().getId()))) {
            Role newRole = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.roleId()));
            user.setRole(newRole);
            syncSubtypeRecord(user, newRole.getName());
        } else if (request.roleName() != null && !request.roleName().isBlank() &&
                (user.getRole() == null || !request.roleName().equalsIgnoreCase(user.getRole().getName()))) {
            Role newRole = roleRepository.findByNameIgnoreCase(request.roleName().trim())
                    .orElseThrow(() -> new BusinessException("Role not found with name: " + request.roleName()));
            user.setRole(newRole);
            syncSubtypeRecord(user, newRole.getName());
        }

        User savedUser = userRepository.save(user);
        auditService.log(null, "UPDATE_USER", "USER", savedUser.getId(),
                "Updated user " + savedUser.getFullName() + " (" + savedUser.getEmail() + ")");

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserStatus(Integer id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String previousStatus = user.getStatus();
        String newStatus = normalizeUserStatus(status);
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        auditService.log(null, "CHANGE_USER_STATUS", "USER", savedUser.getId(),
                previousStatus + " -> " + newStatus);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserRole(Integer id, Integer roleId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        String previousRole = user.getRole() != null ? user.getRole().getName() : "None";
        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        syncSubtypeRecord(savedUser, newRole.getName());

        auditService.log(null, "CHANGE_USER_ROLE", "USER", savedUser.getId(),
                previousRole + " -> " + newRole.getName());

        return UserResponse.from(savedUser);
    }

    private void syncSubtypeRecord(User user, String roleName) {
        if ("Coach".equalsIgnoreCase(roleName)) {
            if (!coachRepository.existsById(user.getId())) {
                Coach coach = new Coach();
                coach.setUser(user);
                coach.setSpecialization("Fitness");
                coachRepository.save(coach);
            }
        } else if ("Receptionist".equalsIgnoreCase(roleName)) {
            if (!receptionistRepository.existsById(user.getId())) {
                Receptionist receptionist = new Receptionist();
                receptionist.setUser(user);
                receptionistRepository.save(receptionist);
            }
        } else if ("Member".equalsIgnoreCase(roleName)) {
            if (!memberRepository.existsById(user.getId())) {
                Member member = new Member();
                member.setUser(user);
                member.setJoinDate(LocalDate.now());
                memberRepository.save(member);
            }
        }
    }

    private String normalizeUserStatus(String status) {
        if ("suspended".equalsIgnoreCase(status) || "locked".equalsIgnoreCase(status)) return "Locked";
        if ("inactive".equalsIgnoreCase(status)) return "Inactive";
        return "Active";
    }
}
