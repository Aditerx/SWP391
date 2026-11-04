package com.sportscenter.auth;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${scms.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;
    @Value("${spring.mail.username:}")
    private String smtpUsername;
    @Value("${spring.mail.password:}")
    private String appPassword;
    @Value("${spring.mail.host:}")
    private String smtpHost;
    @Value("${spring.mail.port:587}")
    private int smtpPort;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnabled;

    public boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank()
                && smtpUsername != null && !smtpUsername.isBlank()
                && appPassword != null && !appPassword.isBlank();
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
        String attemptId = UUID.randomUUID().toString();
        String host = smtpHost != null && smtpHost.matches("[A-Za-z0-9.-]{1,253}") ? smtpHost : "unset-or-invalid";
        log.info("SMTP_SEND_START attemptId={} host={} port={} starttls={} loginConfigured={} passwordConfigured={} senderConfigured={}",
                attemptId, host, smtpPort, starttlsEnabled, hasText(smtpUsername), hasText(appPassword), hasText(fromAddress));
        if (!hasText(fromAddress)) {
            log.warn("SMTP_SEND_FAILED attemptId={} category=MISSING_SENDER", attemptId);
            return false;
        }
        long started = System.nanoTime();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("SMTP_SEND_ACCEPTED attemptId={} elapsedMs={}", attemptId, elapsedMillis(started));
            return true;
        } catch (MailException exception) {
            MailFailure failure = describeFailure(exception);
            // Never log the exception/message: JavaMail may embed recipients, body or credentials.
            log.warn("SMTP_SEND_FAILED attemptId={} host={} port={} starttls={} category={} smtpCode={} exceptionTypes={} elapsedMs={}",
                    attemptId, host, smtpPort, starttlsEnabled, failure.category(), failure.smtpCode(),
                    failure.exceptionTypes(), elapsedMillis(started));
            return false;
        }
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private record MailFailure(String category, int smtpCode, String exceptionTypes) {}

    private static MailFailure describeFailure(MailException failure) {
        // Inspect both JavaMail's nextException and Spring's per-message failures, without rendering either.
        var pending = new ArrayDeque<Throwable>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> types = new LinkedHashSet<>();
        pending.add(failure);
        String category = "UNKNOWN";
        int priority = Integer.MAX_VALUE;
        int smtpCode = -1;
        for (int inspected = 0; !pending.isEmpty() && inspected < 32; inspected++) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) continue;
            types.add(current.getClass().getSimpleName());
            String candidate = "UNKNOWN";
            int candidatePriority = Integer.MAX_VALUE;
            if (current instanceof SSLException) { candidate = "TLS"; candidatePriority = 0; }
            else if (current instanceof MailAuthenticationException || current instanceof AuthenticationFailedException) {
                candidate = "AUTHENTICATION"; candidatePriority = 1;
            } else if (current instanceof UnknownHostException) { candidate = "DNS"; candidatePriority = 2; }
            else if (current instanceof SocketTimeoutException) { candidate = "TIMEOUT"; candidatePriority = 3; }
            else if (current instanceof ConnectException || current instanceof NoRouteToHostException) {
                candidate = "CONNECTION"; candidatePriority = 4;
            } else if (current instanceof SMTPAddressFailedException || current instanceof SMTPSendFailedException
                    || current instanceof SMTPSenderFailedException) {
                candidate = "SMTP_REJECTED"; candidatePriority = 5;
            } else if (current instanceof MailParseException) { candidate = "MESSAGE_FORMAT"; candidatePriority = 6; }
            if (candidatePriority < priority) { category = candidate; priority = candidatePriority; }
            if (current instanceof SMTPAddressFailedException smtp) smtpCode = smtp.getReturnCode();
            else if (current instanceof SMTPSendFailedException smtp) smtpCode = smtp.getReturnCode();
            else if (current instanceof SMTPSenderFailedException smtp) smtpCode = smtp.getReturnCode();
            if (current.getCause() != null) pending.addLast(current.getCause());
            if (current instanceof MessagingException messaging && messaging.getNextException() != null)
                pending.addLast(messaging.getNextException());
            if (current instanceof MailSendException sendFailure)
                for (Exception messageFailure : sendFailure.getMessageExceptions())
                    if (messageFailure != null) pending.addLast(messageFailure);
        }
        return new MailFailure(category, smtpCode, String.join(",", types));
    }

    private String safe(String value) { return value == null ? "" : value; }
}
