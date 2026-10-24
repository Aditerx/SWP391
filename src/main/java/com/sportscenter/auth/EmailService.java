package com.sportscenter.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;
    @Value("${spring.mail.password:}")
    private String appPassword;

    public boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank() && appPassword != null && !appPassword.isBlank();
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(OtpMailEvent event) {
        if (!sendOtpEmail(event.email(), event.otp(), event.purpose()))
            log.warn("OTP email delivery failed for purpose {}", event.purpose());
    }

    public boolean sendWelcomeEmail(String email, String fullName, String temporaryPassword) {
        return send(email, "Tài khoản Sports Center của bạn",
                "Xin chào " + safe(fullName) + ",\n\nTài khoản đăng nhập của bạn đã được tạo.\nEmail: " + email
                        + "\nMật khẩu tạm thời: " + temporaryPassword + "\n\nVui lòng đăng nhập và đổi mật khẩu ngay lần đầu sử dụng.");
    }

    public boolean sendOtpEmail(String email, String otp, String purpose) {
        boolean registration = "REGISTER".equals(purpose);
        String action = registration ? "xác thực đăng ký" : "đặt lại mật khẩu";
        String subject = registration ? "Mã OTP xác thực đăng ký" : "Mã OTP đặt lại mật khẩu";
        return send(email, subject, "Mã OTP " + action + " của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 10 phút. Không chia sẻ mã này với bất kỳ ai. Nếu bạn không yêu cầu mã, hãy bỏ qua email.");
    }

    private boolean send(String recipient, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) return false;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            return false;
        }
    }

    private String safe(String value) { return value == null ? "" : value; }
}
