package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.user.dto.MemberRequest;
import com.sportscenter.user.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<MemberResponse> findAllMembers() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "Member".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        Map<Integer, Member> memberMap = memberRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(Member::getUserId, m -> m));

        Map<Integer, List<MemberPackage>> packageMap = memberPackageRepository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(mp -> mp.getMember().getId()));

        return users.stream().map(u -> {
            Member member = memberMap.get(u.getId());
            List<MemberPackage> packages = packageMap.getOrDefault(u.getId(), List.of());
            MemberPackage activePkg = packages.stream()
                    .filter(p -> "Active".equalsIgnoreCase(p.getStatus()))
                    .findFirst()
                    .orElse(packages.isEmpty() ? null : packages.get(packages.size() - 1));

            BigDecimal totalSpent = packages.stream()
                    .map(p -> p.getMembershipPackage() != null ? p.getMembershipPackage().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return MemberResponse.from(u, member, activePkg, totalSpent);
        }).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + id));
        Member member = memberRepository.findById(id).orElse(null);
        List<MemberPackage> packages = memberPackageRepository.findByMemberId(id);
        MemberPackage activePkg = packages.stream()
                .filter(p -> "Active".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .orElse(packages.isEmpty() ? null : packages.get(packages.size() - 1));

        BigDecimal totalSpent = packages.stream()
                .map(p -> p.getMembershipPackage() != null ? p.getMembershipPackage().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MemberResponse.from(user, member, activePkg, totalSpent);
    }

    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        if (userRepository.findForAuthentication(request.email()).isPresent()) {
            throw new BusinessException("Email already exists: " + request.email());
        }

        Role memberRole = roleRepository.findAll().stream()
                .filter(r -> "Member".equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Member role not found"));

        User user = new User();
        user.setFullName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole(memberRole);
        String rawPassword = (request.password() != null && !request.password().isBlank()) ? request.password() : "Scms@2026";
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(request.status() != null ? normalizeUserStatus(request.status()) : "Active");

        User savedUser = userRepository.save(user);

        Member member = new Member();
        member.setUser(savedUser);
        member.setGoal(request.goal());
        member.setHealthNote(request.healthNote());
        member.setJoinDate(LocalDate.now());

        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            userRepository.findForAuthentication(auth.getName()).ifPresent(member::setRegisteredBy);
        }

        Member savedMember = memberRepository.save(member);


        MemberPackage createdPackage = null;
        if (request.packageId() != null) {
            MembershipPackage pkg = membershipPackageRepository.findById(request.packageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + request.packageId()));

            MemberPackage mp = new MemberPackage();
            mp.setMember(savedUser);
            mp.setMembershipPackage(pkg);
            mp.setStartDate(LocalDate.now());
            int days = pkg.getDurationDays() != null ? pkg.getDurationDays() : 30;
            mp.setEndDate(LocalDate.now().plusDays(days));
            mp.setStatus("Active");
            createdPackage = memberPackageRepository.save(mp);
        }

        auditService.log(null, "CREATE_MEMBER", "USER", savedUser.getId(), savedUser.getFullName());
        BigDecimal totalSpent = createdPackage != null && createdPackage.getMembershipPackage() != null
                ? createdPackage.getMembershipPackage().getPrice() : BigDecimal.ZERO;
        return MemberResponse.from(savedUser, savedMember, createdPackage, totalSpent);
    }

    @Transactional
    public MemberResponse updateMember(Integer id, MemberRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + id));

        user.setFullName(request.name());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) user.setAddress(request.address());
        if (request.gender() != null) user.setGender(request.gender());
        if (request.dateOfBirth() != null) user.setDateOfBirth(request.dateOfBirth());
        if (request.status() != null) user.setStatus(normalizeUserStatus(request.status()));
        User savedUser = userRepository.save(user);

        Member member = memberRepository.findById(id).orElse(null);
        if (member != null) {
            if (request.goal() != null) member.setGoal(request.goal());
            if (request.healthNote() != null) member.setHealthNote(request.healthNote());
            member = memberRepository.save(member);
        }

        auditService.log(null, "UPDATE_MEMBER", "USER", savedUser.getId(), savedUser.getFullName());
        return findById(id);
    }

    @Transactional
    public MemberResponse updateMemberStatus(Integer id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found: " + id));
        String previousStatus = user.getStatus();
        String newStatus = normalizeUserStatus(status);
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);

        auditService.log(null, "CHANGE_MEMBER_STATUS", "USER", savedUser.getId(), previousStatus + " -> " + newStatus);
        return findById(id);
    }

    private String normalizeUserStatus(String status) {
        if ("suspended".equalsIgnoreCase(status) || "locked".equalsIgnoreCase(status)) return "Locked";
        if ("inactive".equalsIgnoreCase(status)) return "Inactive";
        return "Active";
    }
}
