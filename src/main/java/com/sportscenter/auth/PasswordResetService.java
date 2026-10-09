package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    public static final String GENERIC_FORGOT_MESSAGE =
            "Nếu email hợp lệ, hướng dẫn đặt lại mật khẩu sẽ được gửi đến hộp thư.";
    private static final String PURPOSE = "RESET_PASSWORD";
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final SessionRegistry sessionRegistry;
    private final UserDetailsService userDetailsService;

    @Transactional
    public void requestReset(String rawEmail) {
        String email = normalize(rawEmail);
        if (otpRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                email, PURPOSE, LocalDateTime.now().minusHours(1)) >= 3) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(item -> "Active".equalsIgnoreCase(item.getStatus())).orElse(null);
        if (user == null) {
            storeQuotaOnly(email);
            return;
        }
        String otp = generateOtp();
        storeOtp(email, user, otp, PURPOSE);
        emailService.sendOtpEmail(email, otp, PURPOSE);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void reset(ResetPasswordRequest request) {
        String email = normalize(request.email());
        List<EmailOtp> codes = otpRepository.findUnusedForUpdate(email, PURPOSE);
        EmailOtp otp = codes.isEmpty() ? null : codes.get(0);
        if (otp == null || otp.getUser() == null) {
            throw new BusinessException("OTP is invalid or expired");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException("OTP is invalid or expired");
        }
        if (!passwordEncoder.matches(request.otp(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= 3) otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException("OTP is invalid or expired");
        }

        User user = otp.getUser();
        if (!PASSWORD_POLICY.matcher(request.newPassword()).matches()) {
            throw new BusinessException("New password must be at least 8 characters and include letters and numbers");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFirstLogin(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        otp.setUsed(true);
        otpRepository.save(otp);
        for (EmailOtp other : codes) {
            other.setUsed(true);
            if (!other.getId().equals(otp.getId())) otpRepository.save(other);
        }
        var principal = userDetailsService.loadUserByUsername(user.getEmail());
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
            session.expireNow();
        }
        auditService.log(user.getId(), "RESET_PASSWORD", "USER", user.getId(),
                "Password reset using email OTP");
    }

    private void storeOtp(String email, User user, String rawOtp, String purpose) {
        otpRepository.findUnusedForUpdate(email, purpose).forEach(previous -> {
            previous.setUsed(true);
            otpRepository.save(previous);
        });
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setUser(user);
        otp.setPurpose(purpose);
        otp.setOtpHash(passwordEncoder.encode(rawOtp));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otp.setAttempts(0);
        otp.setUsed(false);
        otpRepository.save(otp);
    }

    private void storeQuotaOnly(String email) {
        EmailOtp marker = new EmailOtp();
        marker.setEmail(email);
        marker.setPurpose(PURPOSE);
        marker.setOtpHash(passwordEncoder.encode("quota-marker-" + RANDOM.nextLong()));
        marker.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        marker.setAttempts(0);
        marker.setUsed(true);
        otpRepository.save(marker);
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
