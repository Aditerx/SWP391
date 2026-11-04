package com.sportscenter.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceConfigurationTest {
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
    void verifiedSenderIsUsedInsteadOfSmtpLogin() {
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
}
