package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.Role;
import com.sportscenter.user.RoleRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private static final String PURPOSE = "REGISTER";
    private static final String GENERIC_MESSAGE = "Nếu thông tin hợp lệ, mã xác thực sẽ được gửi đến hộp thư.";
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final EmailOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;

    @Transactional
    public OtpMessageResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if ("Pending".equalsIgnoreCase(existing.getStatus())) issueOtp(email, existing);
            return new OtpMessageResponse(GENERIC_MESSAGE);
        }
        if (!PASSWORD_POLICY.matcher(request.password()).matches()) {
            throw new BusinessException("Password must be at least 8 characters and include letters and numbers");
        }

        Role memberRole = roleRepository.findByNameIgnoreCase("Member")
                .orElseThrow(() -> new BusinessException("Member role not found"));
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender());
        user.setRole(memberRole);
        user.setCenterId(1);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("Pending");
        user.setFirstLogin(false);
        User saved = userRepository.save(user);

        Member member = new Member();
        member.setUser(saved);
        member.setJoinDate(LocalDate.now());
        memberRepository.save(member);
        auditService.log(saved.getId(), "REGISTER_MEMBER_PENDING", "USER", saved.getId(),
                "Member self-registration is awaiting email verification");
        issueOtp(email, saved);
        return new OtpMessageResponse(GENERIC_MESSAGE);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(VerifyRegistrationRequest request) {
        String email = normalize(request.email());
        List<EmailOtp> codes = otpRepository.findUnusedForUpdate(email, PURPOSE);
        EmailOtp otp = codes.isEmpty() ? null : codes.get(0);
        if (otp == null || otp.getUser() == null || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP is invalid or expired");
        }
        if (!passwordEncoder.matches(request.otp(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= 3) otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException("OTP is invalid or expired");
        }
        User user = otp.getUser();
        if (!"Pending".equalsIgnoreCase(user.getStatus())) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return;
        }
        user.setStatus("Active");
        userRepository.save(user);
        codes.forEach(code -> {
            code.setUsed(true);
            otpRepository.save(code);
        });
        auditService.log(user.getId(), "VERIFY_MEMBER_REGISTRATION", "USER", user.getId(),
                "Member email verification completed");
    }

    @Transactional
    public void resend(ResendOtpRequest request) {
        String purpose = request.purpose().trim().toUpperCase();
        if ("RESET_PASSWORD".equals(purpose)) {
            passwordResetService.requestReset(request.email());
            return;
        }
        if (!PURPOSE.equals(purpose)) throw new BusinessException("Unsupported OTP purpose");
        String email = normalize(request.email());
        if (otpRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                email, PURPOSE, LocalDateTime.now().minusHours(1)) >= 3) {
            throw new BusinessException("OTP request limit reached. Try again later.");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(item -> "Pending".equalsIgnoreCase(item.getStatus())).orElse(null);
        if (user != null) issueOtp(email, user);
    }

    private void issueOtp(String email, User user) {
        if (otpRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                email, PURPOSE, LocalDateTime.now().minusHours(1)) >= 3) return;
        otpRepository.findUnusedForUpdate(email, PURPOSE).forEach(previous -> {
            previous.setUsed(true);
            otpRepository.save(previous);
        });
        String rawOtp = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setUser(user);
        otp.setPurpose(PURPOSE);
        otp.setOtpHash(passwordEncoder.encode(rawOtp));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otp.setAttempts(0);
        otp.setUsed(false);
        otpRepository.save(otp);
        emailService.sendOtpEmail(email, rawOtp, PURPOSE);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
