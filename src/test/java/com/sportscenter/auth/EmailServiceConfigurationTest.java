package com.sportscenter.auth;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class EmailServiceConfigurationTest {
    private ch.qos.logback.classic.Logger smtpLogger;
    private ch.qos.logback.classic.Level previousSmtpLogLevel;

    @BeforeEach
    void enableSmtpInfoLogs() {
        // Cached Spring contexts may leave the root logger at WARN. This fixture asserts INFO events.
        smtpLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(EmailService.class);
        previousSmtpLogLevel = smtpLogger.getLevel();
        smtpLogger.setLevel(ch.qos.logback.classic.Level.INFO);
    }

    @AfterEach
    void restoreSmtpLogLevel() {
        smtpLogger.setLevel(previousSmtpLogLevel);
    }

    private static final String PRIVATE_DETAILS = "private-smtp-key recipient@example.com secret-server-reply";
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources().remove("systemEnvironment"))
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withBean(JavaMailSender.class, () -> sender)
            .withUserConfiguration(EmailService.class)
            .withPropertyValues("spring.config.import=", "MAIL_USERNAME=login@relay.example",
                    "MAIL_APP_PASSWORD=local-test-key");

    @Test
    void verifiedSenderIsUsedInsteadOfSmtpLogin(CapturedOutput output) {
        contextRunner.withPropertyValues("MAIL_FROM_EMAIL=verified@example.com").run(context -> {
            EmailService service = context.getBean(EmailService.class);
            assertTrue(service.isConfigured());
            assertTrue(service.sendOtpEmail("member@example.com", "123456", "REGISTER"));
            ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(sender).send(message.capture());
            assertEquals("verified@example.com", message.getValue().getFrom());
            assertArrayEquals(new String[]{"member@example.com"}, message.getValue().getTo());
            assertTrue(message.getValue().getText().contains("123456"));
            assertEquals("login@relay.example", context.getEnvironment().getProperty("spring.mail.username"));
        });
        assertTrue(output.getAll().contains("SMTP_SEND_START attemptId="));
        assertTrue(output.getAll().contains("SMTP_SEND_ACCEPTED attemptId="));
        assertFalse(output.getAll().contains("member@example.com"));
        assertFalse(output.getAll().contains("123456"));
    }

    @Test
    void existingSmtpConfigurationStillUsesLoginAsSenderWhenOverrideIsAbsent() {
        contextRunner.run(context -> {
            EmailService service = context.getBean(EmailService.class);
            assertTrue(service.isConfigured());
            assertTrue(service.sendOtpEmail("member@example.com", "123456", "RESET_PASSWORD"));
            ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(sender).send(message.capture());
            assertEquals("login@relay.example", message.getValue().getFrom());
        });
    }

    @Test
    void senderAddressAloneDoesNotMakeMissingSmtpCredentialsConfigured() {
        contextRunner.withPropertyValues("MAIL_FROM_EMAIL=verified@example.com", "MAIL_USERNAME=")
                .run(context -> assertFalse(context.getBean(EmailService.class).isConfigured()));
        contextRunner.withPropertyValues("MAIL_FROM_EMAIL=verified@example.com", "MAIL_APP_PASSWORD=")
                .run(context -> assertFalse(context.getBean(EmailService.class).isConfigured()));
        verifyNoInteractions(sender);
    }

    @ParameterizedTest
    @MethodSource("smtpFailures")
    void failureLogsSafeCategoryAndSmtpCodeWithoutMessagesOrMailContent(
            MailException failure, String category, int smtpCode, CapturedOutput output) {
        doThrow(failure).when(sender).send(any(SimpleMailMessage.class));
        contextRunner.withPropertyValues("MAIL_HOST=smtp-relay.example", "MAIL_PORT=2525",
                "MAIL_FROM_EMAIL=verified@example.com").run(context -> {
            assertFalse(context.getBean(EmailService.class)
                    .sendOtpEmail("recipient@example.com", "654321", "REGISTER"));
        });
        String logs = output.getAll();
        assertTrue(logs.contains("SMTP_SEND_FAILED attemptId="));
        assertTrue(logs.contains("host=smtp-relay.example port=2525 starttls=true"));
        assertTrue(logs.contains("category=" + category + " smtpCode=" + smtpCode));
        assertTrue(logs.contains("exceptionTypes="));
        for (String privateValue : new String[]{"private-smtp-key", "recipient@example.com", "secret-server-reply",
                "verified@example.com", "login@relay.example", "local-test-key", "654321"})
            assertFalse(logs.contains(privateValue), "Log leaked sensitive mail data");
        assertFalse(logs.contains("SMTP_SEND_ACCEPTED"));
    }

    private static Stream<Arguments> smtpFailures() throws Exception {
        SimpleMailMessage privateMessage = new SimpleMailMessage();
        privateMessage.setTo("recipient@example.com");
        privateMessage.setText("private-smtp-key 654321");
        MessagingException perMessageFailure = new MessagingException(PRIVATE_DETAILS);
        perMessageFailure.setNextException(new SMTPAddressFailedException(
                new InternetAddress("recipient@example.com"), "RCPT TO:<recipient@example.com>", 550, PRIVATE_DETAILS));
        return Stream.of(
                Arguments.of(new MailAuthenticationException(PRIVATE_DETAILS,
                        new AuthenticationFailedException(PRIVATE_DETAILS)), "AUTHENTICATION", -1),
                Arguments.of(wrapped(new UnknownHostException(PRIVATE_DETAILS)), "DNS", -1),
                Arguments.of(wrapped(new SocketTimeoutException(PRIVATE_DETAILS)), "TIMEOUT", -1),
                Arguments.of(wrapped(new ConnectException(PRIVATE_DETAILS)), "CONNECTION", -1),
                Arguments.of(wrapped(new SSLHandshakeException(PRIVATE_DETAILS)), "TLS", -1),
                Arguments.of(new MailSendException(Map.of(privateMessage, perMessageFailure)), "SMTP_REJECTED", 550),
                Arguments.of(wrapped(new SMTPSenderFailedException(new InternetAddress("verified@example.com"),
                        "MAIL FROM:<verified@example.com>", 553, PRIVATE_DETAILS)), "SMTP_REJECTED", 553),
                Arguments.of(wrapped(new SMTPSendFailedException("DATA", 451, PRIVATE_DETAILS,
                        null, null, null, null)), "SMTP_REJECTED", 451),
                Arguments.of(new MailSendException(PRIVATE_DETAILS), "UNKNOWN", -1));
    }

    private static MailSendException wrapped(Exception cause) {
        return new MailSendException(PRIVATE_DETAILS, new MessagingException(PRIVATE_DETAILS, cause));
    }
}
