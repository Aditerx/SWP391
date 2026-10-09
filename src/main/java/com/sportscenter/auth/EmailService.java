package com.sportscenter.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public boolean sendWelcomeEmail(String email, String fullName, String temporaryPassword) {
        return send(email, "Tài khoản Sports Center của bạn",
                "Xin chào " + safe(fullName) + ",\n\n"
                        + "Tài khoản đăng nhập của bạn đã được tạo.\n"
                        + "Email: " + email + "\n"
                        + "Mật khẩu tạm thời: " + temporaryPassword + "\n\n"
                        + "Vui lòng đăng nhập và đổi mật khẩu ngay lần đầu sử dụng.");
    }

    public boolean sendOtpEmail(String email, String otp, String purpose) {
        String action = "REGISTER".equals(purpose) ? "xác thực đăng ký" : "đặt lại mật khẩu";
        return send(email, "Mã OTP " + action,
                "Mã OTP " + action + " của bạn là: " + otp + "\n"
                        + "Mã có hiệu lực trong 10 phút. Nếu bạn không yêu cầu mã này, hãy bỏ qua email.");
    }

    private boolean send(String recipient, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) {
            return false;
        }
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
