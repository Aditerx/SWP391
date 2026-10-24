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
import java.time.Clock;
import java.time.Duration;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final String PURPOSE = "REGISTER";
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS_PER_HOUR = 3;
    private static final long RESEND_WAIT_SECONDS = 60;
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
    private final Clock clock;
    private final ApplicationEventPublisher events;
    @org.springframework.beans.factory.annotation.Value("${REGISTRATION_DEMO_OTP:}")
    private String demoOtp = "";

    @PostConstruct
    void warnIfDemoOtpEnabled() {
        if (demoOtp != null && !demoOtp.isBlank()) log.warn("Registration demo OTP mode is enabled; use only in non-production environments.");
    }

    @Transactional
    public OtpMessageResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if ("Pending".equalsIgnoreCase(existing.getStatus())) {
                issueOtp(email, existing);
                return new OtpMessageResponse("Tài khoản đang chờ xác thực; mã OTP mới đã được gửi.", "REGISTRATION_PENDING");
            }
            throw new BusinessException("EMAIL_ALREADY_REGISTERED: Email đã được đăng ký.");
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
        member.setJoinDate(LocalDate.now(clock));
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
        if (otp == null) {
            boolean lockedByAttempts = otpRepository.findFirstByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, PURPOSE)
                    .map(code -> code.getAttempts() >= MAX_ATTEMPTS).orElse(false);
            if (lockedByAttempts) throw new BusinessException("OTP_ATTEMPTS_EXCEEDED: Vui lòng yêu cầu mã OTP mới.");
            throw new BusinessException("OTP_INVALID: Mã OTP không hợp lệ.");
        }
        if (otp.getUser() == null) throw new BusinessException("OTP_INVALID: Mã OTP không hợp lệ.");
        if (!otp.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            otp.setUsed(true); otpRepository.save(otp);
            throw new BusinessException("OTP_EXPIRED: Mã OTP đã hết hạn.");
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) throw new BusinessException("OTP_ATTEMPTS_EXCEEDED: Vui lòng yêu cầu mã OTP mới.");
        if (!passwordEncoder.matches(request.otp(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= MAX_ATTEMPTS) otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException(otp.isUsed() ? "OTP_ATTEMPTS_EXCEEDED: Vui lòng yêu cầu mã OTP mới." : "OTP_INVALID: Mã OTP không hợp lệ.");
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
            passwordResetService.resend(request.email());
            return;
        }
        if (!PURPOSE.equals(purpose)) throw new BusinessException("Unsupported OTP purpose");
        String email = normalize(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(item -> "Pending".equalsIgnoreCase(item.getStatus())).orElse(null);
        if (user != null) issueOtp(email, user);
    }

    private void issueOtp(String email, User user) {
        if ((demoOtp == null || demoOtp.isBlank()) && !emailService.isConfigured())
            throw new BusinessException("MAIL_NOT_CONFIGURED: Email không thể gửi OTP hiện đang chưa được cấu hình.");
        LocalDateTime now = LocalDateTime.now(clock);
        var latest = otpRepository.findFirstByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, PURPOSE).orElse(null);
        if (latest != null && latest.getCreatedAt() != null) {
            long elapsed = Duration.between(latest.getCreatedAt(), now).getSeconds();
            if (elapsed < RESEND_WAIT_SECONDS) throw new BusinessException("OTP_RESEND_TOO_SOON: " + (RESEND_WAIT_SECONDS - Math.max(0, elapsed)));
        }
        if (otpRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(email, PURPOSE, now.minusHours(1)) >= MAX_RESENDS_PER_HOUR)
            throw new BusinessException("OTP_RESEND_LIMIT: Đã đạt giới hạn gửi OTP trong giờ này.");
        otpRepository.findUnusedForUpdate(email, PURPOSE).forEach(previous -> {
            previous.setUsed(true);
            otpRepository.save(previous);
        });
        String rawOtp = demoOtp != null && !demoOtp.isBlank()
                ? demoOtp : String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setUser(user);
        otp.setPurpose(PURPOSE);
        otp.setOtpHash(passwordEncoder.encode(rawOtp));
        otp.setExpiresAt(now.plusMinutes(10));
        otp.setAttempts(0);
        otp.setUsed(false);
        otpRepository.save(otp);
        if (demoOtp == null || demoOtp.isBlank()) events.publishEvent(new OtpMailEvent(email, rawOtp, PURPOSE));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
