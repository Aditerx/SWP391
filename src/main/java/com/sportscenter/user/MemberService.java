package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.center.CenterContext;
import com.sportscenter.auth.EmailService;
import com.sportscenter.auth.TemporaryPasswordGenerator;
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
    private final java.time.Clock clock;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final com.sportscenter.invoice.InvoiceRepository invoiceRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final CenterContext centerContext;
    private final EmailService emailService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final com.sportscenter.membership.MemberPackageService subscriptionService;

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

            BigDecimal totalSpent = paidTotal(u.getId());

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

        BigDecimal totalSpent = paidTotal(id);

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
        user.setCenterId(centerContext.centerForNewAccount(request.centerId(), false));
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(temporaryPasswordGenerator.hash(temporaryPassword));
        user.setFirstLogin(true);
        user.setStatus(request.status() != null ? normalizeUserStatus(request.status()) : "Active");

        User savedUser = userRepository.save(user);

        Member member = new Member();
        member.setUser(savedUser);
        member.setGoal(request.goal());
        member.setHealthNote(request.healthNote());
        member.setJoinDate(LocalDate.now(clock));

        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            userRepository.findForAuthentication(auth.getName()).ifPresent(member::setRegisteredBy);
        }

        Member savedMember = memberRepository.save(member);


        MemberPackage createdPackage = null;
        if (request.packageId() != null) {
            var order = subscriptionService.subscribe(savedUser.getId(),
                    new com.sportscenter.membership.MemberPackageRequest(request.packageId(), null, null, false, "Cash", null));
            createdPackage = memberPackageRepository.findById(order.subscriptionId()).orElseThrow();
        }

        auditService.log(null, "CREATE_MEMBER", "USER", savedUser.getId(), savedUser.getFullName());
        BigDecimal totalSpent = BigDecimal.ZERO; // Pending orders are not money received.
        boolean emailSent = emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), temporaryPassword);
        return MemberResponse.from(savedUser, savedMember, createdPackage, totalSpent).withEmailDelivery(emailSent,
                emailSent ? null : "Tạo tài khoản thành công nhưng gửi email thất bại. Dùng chức năng cấp lại mật khẩu để gửi lại.");
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
    private BigDecimal paidTotal(Integer memberId) {
        return invoiceRepository.findByMemberId(memberId).stream()
                .filter(i -> "Paid".equals(i.getPaymentStatus()) && i.getAmount() != null)
                .map(com.sportscenter.invoice.Invoice::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
    }

}
