package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.center.CenterContext;
import com.sportscenter.auth.EmailService;
import com.sportscenter.auth.TemporaryPasswordGenerator;
import com.sportscenter.specialization.CoachSpecializationAssignmentService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.dto.UserRequest;
import com.sportscenter.user.dto.UserResponse;
import com.sportscenter.user.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CoachRepository coachRepository;
    private final ReceptionistRepository receptionistRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CenterContext centerContext;
    private final EmailService emailService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final SessionRegistry sessionRegistry;
    private final UserDetailsService userDetailsService;
    private final CoachSpecializationAssignmentService coachSpecializationAssignmentService;

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
        assertCanManageAdminRole(null, role);

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole(role);
        user.setCenterId(centerContext.centerForNewAccount(request.centerId(),
                "Admin".equalsIgnoreCase(role.getName())));
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(temporaryPasswordGenerator.hash(temporaryPassword));
        user.setFirstLogin(true);
        user.setStatus(request.status() != null ? normalizeUserStatus(request.status()) : "Active");

        User savedUser = userRepository.save(user);

        // Sync subtype table if necessary
        syncSubtypeRecord(savedUser, role.getName());
        if ("Coach".equalsIgnoreCase(role.getName()) && request.specializationIds() != null) {
            coachRepository.findById(savedUser.getId())
                    .ifPresent(coach -> coachSpecializationAssignmentService.assign(coach, request.specializationIds()));
        }

        auditService.log(null, "CREATE_USER", "USER", savedUser.getId(),
                "Created user " + savedUser.getFullName() + " (" + savedUser.getEmail() + ") with role " + role.getName());
        boolean emailSent = emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), temporaryPassword);
        return UserResponse.from(savedUser).withEmailDelivery(emailSent,
                emailSent ? null : "Tạo tài khoản thành công nhưng gửi email thất bại. Dùng chức năng cấp lại mật khẩu để gửi lại.");
    }

    @Transactional
    public UserResponse updateUser(Integer id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        assertCanManageAdminRole(user, null);

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
            assertCanManageAdminRole(user, newRole);
            user.setRole(newRole);
            syncSubtypeRecord(user, newRole.getName());
        } else if (request.roleName() != null && !request.roleName().isBlank() &&
                (user.getRole() == null || !request.roleName().equalsIgnoreCase(user.getRole().getName()))) {
            Role newRole = roleRepository.findByNameIgnoreCase(request.roleName().trim())
                    .orElseThrow(() -> new BusinessException("Role not found with name: " + request.roleName()));
            assertCanManageAdminRole(user, newRole);
            user.setRole(newRole);
            syncSubtypeRecord(user, newRole.getName());
        }

        User savedUser = userRepository.save(user);
        if (savedUser.getRole() != null && "Coach".equalsIgnoreCase(savedUser.getRole().getName())
                && request.specializationIds() != null) {
            coachRepository.findById(savedUser.getId())
                    .ifPresent(coach -> coachSpecializationAssignmentService.assign(coach, request.specializationIds()));
        }
        auditService.log(null, "UPDATE_USER", "USER", savedUser.getId(),
                "Updated user " + savedUser.getFullName() + " (" + savedUser.getEmail() + ")");

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserStatus(Integer id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        assertCanManageAdminRole(user, null);

        String previousStatus = user.getStatus();
        String newStatus = normalizeUserStatus(status);
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        auditService.log(null, "CHANGE_USER_STATUS", "USER", savedUser.getId(),
                previousStatus + " -> " + newStatus);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public User changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (!PASSWORD_POLICY.matcher(request.newPassword()).matches()) {
            throw new BusinessException("New password must be at least 8 characters and include letters and numbers");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false);
        User saved = userRepository.save(user);
        auditService.log(saved.getId(), "CHANGE_PASSWORD", "USER", saved.getId(),
                "User changed their password");
        return saved;
    }

    @Transactional
    public UserResponse reissuePassword(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        Authentication actor = SecurityContextHolder.getContext().getAuthentication();
        boolean canManageUsers = hasAuthority(actor, "MANAGE_USERS");
        boolean canRegisterMember = hasAuthority(actor, "REGISTER_MEMBER");
        boolean canManagePermissions = hasAuthority(actor, "MANAGE_PERMISSIONS");
        User actingUser = actor == null ? null : userRepository.findByEmailIgnoreCase(actor.getName()).orElse(null);
        boolean targetIsAdmin = user.getRole() != null && "Admin".equalsIgnoreCase(user.getRole().getName());
        boolean targetIsMember = user.getRole() != null && "Member".equalsIgnoreCase(user.getRole().getName());

        if (targetIsAdmin ? !canManagePermissions : !(canManageUsers || (canRegisterMember && targetIsMember))) {
            throw new AccessDeniedException("Insufficient permission to reissue this account password");
        }
        if (actingUser != null && actingUser.getRole() != null
                && !"Admin".equalsIgnoreCase(actingUser.getRole().getName())
                && !java.util.Objects.equals(actingUser.getCenterId(), user.getCenterId())) {
            throw new AccessDeniedException("User is outside the current center scope");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(temporaryPasswordGenerator.hash(temporaryPassword));
        user.setFirstLogin(true);
        User saved = userRepository.save(user);
        var principal = userDetailsService.loadUserByUsername(saved.getEmail());
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
            session.expireNow();
        }
        boolean emailSent = emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName(), temporaryPassword);
        auditService.log(null, "REISSUE_TEMPORARY_PASSWORD", "USER", saved.getId(),
                "Temporary password reissued");
        return UserResponse.from(saved).withEmailDelivery(emailSent,
                emailSent ? null : "Tạo tài khoản thành công nhưng gửi email thất bại. Dùng chức năng cấp lại mật khẩu để gửi lại.");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    @Transactional
    public UserResponse updateUserRole(Integer id, Integer roleId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        assertCanManageAdminRole(user, newRole);

        String previousRole = user.getRole() != null ? user.getRole().getName() : "None";
        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        syncSubtypeRecord(savedUser, newRole.getName());

        auditService.log(null, "CHANGE_USER_ROLE", "USER", savedUser.getId(),
                previousRole + " -> " + newRole.getName());

        return UserResponse.from(savedUser);
    }

    private void assertCanManageAdminRole(User currentUser, Role requestedRole) {
        boolean touchesAdminRole = (currentUser != null && currentUser.getRole() != null
                && "Admin".equalsIgnoreCase(currentUser.getRole().getName()))
                || (requestedRole != null && "Admin".equalsIgnoreCase(requestedRole.getName()));
        if (!touchesAdminRole) {
            return;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean canManagePermissions = authentication != null && authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("MANAGE_PERMISSIONS"));
        if (!canManagePermissions) {
            throw new AccessDeniedException("Only administrators with MANAGE_PERMISSIONS may manage Admin accounts");
        }
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
