package com.sportscenter.communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.auth.OtpMailEvent;
import com.sportscenter.notification.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in proof using actual HTTP cookies, application beans, PostgreSQL and SMTP sockets. */
@EnabledIfSystemProperty(named = "scms.communication.proof", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.config.import=", "logging.level.root=WARN", "debug=false"})
@Import(CommunicationFeaturesPostgresTest.NoAutomaticJobs.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommunicationFeaturesPostgresTest {
    private static LoopbackSmtpCapture smtp;
    private static final List<Map<String, Object>> evidence = new CopyOnWriteArrayList<>();
    private static final String PASSWORD = "LocalProof123-" + UUID.randomUUID();
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactions;
    @Autowired NotificationService notifications;

    @DynamicPropertySource
    static void isolatedServices(DynamicPropertyRegistry p) throws Exception {
        String url = System.getProperty("scms.communication.db-url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/scms_communication_[a-z0-9_]+"))
            throw new IllegalArgumentException("Proof accepts only its isolated loopback PostgreSQL database");
        smtp = new LoopbackSmtpCapture();
        p.add("spring.datasource.url", () -> url);
        p.add("spring.datasource.username", () -> "postgres");
        p.add("spring.datasource.password", () -> System.getProperty("scms.communication.db-password", "local-proof-only"));
        p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        p.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        p.add("spring.sql.init.mode", () -> "never");
        p.add("spring.mail.host", () -> "127.0.0.1");
        p.add("spring.mail.port", smtp::port);
        p.add("spring.mail.username", () -> "sender@proof.invalid");
        p.add("spring.mail.password", () -> "local-proof-only");
        p.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        p.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        p.add("REGISTRATION_DEMO_OTP", () -> "");
        p.add("registration.demo-otp", () -> "");
        p.add("server.address", () -> "127.0.0.1");
    }
    @TestConfiguration
    static class NoAutomaticJobs {
        @Bean static BeanFactoryPostProcessor stopUnrelatedScheduledJobs() {
            return factory -> {
                if (factory instanceof BeanDefinitionRegistry registry) {
                    String name = "org.springframework.context.annotation.internalScheduledAnnotationProcessor";
                    if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
                }
            };
        }
    }
    record Reply(int status, JsonNode body) {}
    record Actor(int id, String email, HttpClient http) {}

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }
    private Reply request(HttpClient client, String method, String path, Object body, int expected) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15));
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode parsed = response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
        Map<String,Object> safe = new LinkedHashMap<>();
        safe.put("kind", "real_http"); safe.put("method", method); safe.put("path", path);
        safe.put("expectedStatus", expected); safe.put("actualStatus", response.statusCode());
        for (String field : List.of("code", "id", "userId", "unreadCount", "status", "senderType"))
            if (parsed.has(field)) safe.put(field, parsed.get(field));
        evidence.add(safe);
        assertEquals(expected, response.statusCode(), method + " " + path + " HTTP status");
        return new Reply(response.statusCode(), parsed);
    }
    private String email(String tag) { return "proof-" + tag + "-" + UUID.randomUUID() + "@example.invalid"; }
    private String register(String email) throws Exception {
        request(client(), "POST", "/api/auth/register", Map.of("fullName", "Proof Member", "email", email, "password", PASSWORD), 200);
        String body = smtp.awaitText(email, 1);
        var match = Pattern.compile("(?<![0-9])[0-9]{6}(?![0-9])").matcher(body);
        assertTrue(match.find(), "OTP must arrive in a genuine locally captured SMTP message");
        evidence.add(Map.of("kind", "smtp_capture", "mailbox", "loopback-only", "otpLength", 6, "plaintextOtpRecorded", false));
        return match.group();
    }
    private Actor login(String email, String password) throws Exception {
        HttpClient http = client();
        var response = request(http, "POST", "/api/auth/login", Map.of("email", email, "password", password), 200);
        request(http, "GET", "/api/auth/me", null, 200);
        int id = response.body.path("userId").asInt();
        assertTrue(id > 0, "LoginResponse.userId must identify the authenticated account");
        return new Actor(id, email, http);
    }
    private Actor member(String tag) throws Exception {
        String email = email(tag); String otp = register(email);
        request(client(), "POST", "/api/auth/verify-registration", Map.of("email", email, "otp", otp), 204);
        return login(email, PASSWORD);
    }
    private Actor staff(String role) throws Exception {
        String email = email(role.toLowerCase(Locale.ROOT));
        db.update("INSERT INTO users(role_id,center_id,full_name,email,password_hash,status,is_first_login) "
                + "SELECT role_id,1,'Local proof staff',?,?, 'Active',FALSE FROM roles WHERE role_name=?", email, encoder.encode(PASSWORD), role);
        return login(email, PASSWORD);
    }
    private long count(String sql, Object... args) { return db.queryForObject(sql, Long.class, args); }
    private void databaseProof(String assertion, Object value) { evidence.add(Map.of("kind", "database_assertion", "assertion", assertion, "observed", value)); }

    @Test void registrationUsesSmtpAndHashedOtpThenRealLogin() throws Exception {
        String email = email("registration"); String otp = register(email);
        assertEquals("Pending", db.queryForObject("SELECT status FROM users WHERE email=?", String.class, email));
        String hash = db.queryForObject("SELECT otp_hash FROM email_otps WHERE email=? ORDER BY otp_id DESC LIMIT 1", String.class, email);
        assertNotEquals(otp, hash); assertTrue(encoder.matches(otp, hash));
        var pending = request(client(), "POST", "/api/auth/login", Map.of("email", email, "password", PASSWORD), 400);
        assertEquals("REGISTRATION_PENDING", pending.body.path("code").asText());
        request(client(), "POST", "/api/auth/login", Map.of("email", email, "password", "WrongProof123"), 401);
        request(client(), "POST", "/api/auth/verify-registration", Map.of("email", email, "otp", otp), 204);
        Actor actor = login(email, PASSWORD); assertTrue(actor.id > 0);
        assertEquals("Active", db.queryForObject("SELECT status FROM users WHERE email=?", String.class, email));
        assertEquals(1, count("SELECT count(*) FROM email_otps WHERE email=? AND used=TRUE", email));
        request(client(), "POST", "/api/auth/verify-registration", Map.of("email", email, "otp", otp), 400);
        databaseProof("registration stored hash, Pending -> Active, one consumed OTP", true);
    }

    @Test void resendExpiryAndAttemptGuardsUsePersistedOtpState() throws Exception {
        String email = email("guards"); String otp = register(email);
        var tooSoon = request(client(), "POST", "/api/auth/resend-otp", Map.of("email", email, "purpose", "REGISTER"), 400);
        assertEquals("OTP_RESEND_TOO_SOON", tooSoon.body.path("code").asText());
        // Isolated test fixture simulates elapsed time; never presented as a real ten-minute wait.
        db.update("UPDATE email_otps SET expires_at=CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE email=?", email);
        var expired = request(client(), "POST", "/api/auth/verify-registration", Map.of("email", email, "otp", otp), 400);
        assertEquals("OTP_EXPIRED", expired.body.path("code").asText());
        String second = email("attempts"); String actual = register(second);
        String wrong = actual.equals("000000") ? "000001" : "000000";
        for (int n=1; n<=5; n++) {
            var rejected = request(client(), "POST", "/api/auth/verify-registration", Map.of("email", second, "otp", wrong), 400);
            assertEquals(n==5 ? "OTP_ATTEMPTS_EXCEEDED" : "OTP_INVALID", rejected.body.path("code").asText());
        }
        assertEquals(5, db.queryForObject("SELECT attempts FROM email_otps WHERE email=?", Integer.class, second));
        assertEquals("Pending", db.queryForObject("SELECT status FROM users WHERE email=?", String.class, second));
        databaseProof("five wrong attempts persisted; account remains Pending", true);
    }

    @Test void chatRoundTripPersistsAndAccessChecksUseRealSessions() throws Exception {
        Actor member = member("chat"), outsider = member("outsider"), receptionist = staff("Receptionist"), coach = staff("Coach");
        request(client(), "GET", "/api/chat/conversations/me/active", null, 401);
        request(coach.http, "GET", "/api/chat/conversations", null, 403);
        int conversation = request(member.http, "POST", "/api/chat/conversations", Map.of("subject", "Local proof"), 200).body.path("id").asInt();
        assertEquals(conversation, request(member.http, "POST", "/api/chat/conversations", Map.of(), 200).body.path("id").asInt());
        request(outsider.http, "GET", "/api/chat/conversations/" + conversation + "/messages", null, 404);
        var sent = request(member.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Hello staff", "clientMessageId", "proof-retry"), 200);
        assertEquals(sent.body.path("id").asInt(), request(member.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Hello staff", "clientMessageId", "proof-retry"), 200).body.path("id").asInt());
        assertEquals(1, count("SELECT count(*) FROM chat_messages WHERE conversation_id=?", conversation));
        request(receptionist.http, "POST", "/api/chat/conversations/" + conversation + "/claim", null, 200);
        request(receptionist.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Hello member", "clientMessageId", "staff-reply"), 200);
        JsonNode messages = request(member.http, "GET", "/api/chat/conversations/" + conversation + "/messages?afterId=0&limit=50", null, 200).body;
        assertEquals(2, messages.size()); assertEquals("STAFF", messages.get(1).path("senderType").asText());
        assertEquals(1, request(member.http, "GET", "/api/chat/me/unread-count", null, 200).body.path("unreadCount").asInt());
        request(member.http, "POST", "/api/chat/conversations/" + conversation + "/read", null, 204);
        assertEquals(0, request(member.http, "GET", "/api/chat/me/unread-count", null, 200).body.path("unreadCount").asInt());
        assertEquals(1, count("SELECT count(*) FROM chat_read_state WHERE conversation_id=? AND user_id=?", conversation, member.id));
        request(receptionist.http, "PATCH", "/api/chat/conversations/" + conversation + "/close", null, 200);
        var closed = request(member.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Closed"), 409);
        assertEquals("CHAT_CONVERSATION_CLOSED", closed.body.path("code").asText());
        request(receptionist.http, "PATCH", "/api/chat/conversations/" + conversation + "/reopen", null, 200);
        databaseProof("chat messages persisted once; read cursor per viewer; close/reopen works", true);
    }

    @Test void chatCreatesNotificationFeedAndOwnerReadState() throws Exception {
        Actor member = member("feed"), outsider = member("feed-other"), staff = staff("Receptionist");
        int conversation = request(member.http, "POST", "/api/chat/conversations", Map.of(), 200).body.path("id").asInt();
        request(member.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Need help"), 200);
        request(staff.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "We can help"), 200);
        assertEquals(1, count("SELECT count(*) FROM notification_recipients r JOIN notifications n USING(notification_id) WHERE r.user_id=? AND n.notification_type='CHAT_STAFF_REPLY' AND n.related_id=?", member.id, conversation));
        var feed = request(member.http, "GET", "/api/notifications/me", null, 200).body;
        assertEquals(1, feed.path("totalElements").asInt());
        int notification = feed.path("content").get(0).path("id").asInt();
        assertEquals(1, request(member.http, "GET", "/api/notifications/me/unread-count", null, 200).body.path("unreadCount").asInt());
        request(outsider.http, "PATCH", "/api/notifications/" + notification + "/read", null, 404);
        request(member.http, "PATCH", "/api/notifications/" + notification + "/read", null, 204);
        assertEquals(0, request(member.http, "GET", "/api/notifications/me/unread-count", null, 200).body.path("unreadCount").asInt());
        assertEquals(1, count("SELECT count(*) FROM notification_recipients WHERE notification_id=? AND user_id=? AND is_read=TRUE AND read_at IS NOT NULL", notification, member.id));
        databaseProof("chat AFTER_COMMIT notification recipient and read timestamp persisted", true);
    }

    @Test void commitRollbackAndDedupeExerciseRealTransactionListeners() throws Exception {
        Actor member = member("transaction");
        String key = "proof-" + UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(List.of(member.id), NotificationType.PAYMENT_SUCCESS, "Transaction probe", "Test-only event, no payment made", "PROOF", member.id, key);
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(event);
            assertEquals(0, count("SELECT count(*) FROM notifications WHERE dedupe_key=?", key));
        });
        assertEquals(1, count("SELECT count(*) FROM notifications WHERE dedupe_key=?", key));
        tx.executeWithoutResult(status -> publisher.publishEvent(event));
        assertEquals(1, count("SELECT count(*) FROM notifications WHERE dedupe_key=?", key));
        String rolledBack = "proof-rollback-" + UUID.randomUUID(), recipient = email("rollback");
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new NotificationEvent(List.of(member.id), NotificationType.PAYMENT_SUCCESS, "Rollback probe", "No real payment", "PROOF", member.id, rolledBack));
            publisher.publishEvent(new OtpMailEvent(recipient, "432109", "REGISTER"));
            status.setRollbackOnly();
        });
        assertEquals(0, count("SELECT count(*) FROM notifications WHERE dedupe_key=?", rolledBack));
        assertEquals(0, smtp.count(recipient));
        databaseProof("real transaction: before commit absent; after commit once; rollback absent; DB dedupe holds", true);
    }

    @Test void concurrentOpenAndMessageRetriesPersistExactlyOnce() throws Exception {
        Actor member = member("race");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> open = () -> request(member.http, "POST", "/api/chat/conversations", Map.of(), 200).body.path("id").asInt();
            var opens = pool.invokeAll(List.of(open, open));
            int conversation = opens.get(0).get(); assertEquals(conversation, opens.get(1).get());
            Callable<Integer> send = () -> request(member.http, "POST", "/api/chat/conversations/" + conversation + "/messages", Map.of("content", "Concurrent retry", "clientMessageId", "same-id"), 200).body.path("id").asInt();
            var sends = pool.invokeAll(List.of(send, send));
            assertEquals(sends.get(0).get(), sends.get(1).get());
            assertEquals(1, count("SELECT count(*) FROM chat_conversations WHERE member_user_id=? AND status='OPEN'", member.id));
            assertEquals(1, count("SELECT count(*) FROM chat_messages WHERE conversation_id=?", conversation));
            databaseProof("concurrent open/retry: exactly one conversation and one message", true);
        } finally { pool.shutdownNow(); }
    }

    @AfterAll void saveSanitizedEvidence() throws Exception {
        if (smtp != null) smtp.close();
        String directory = System.getProperty("scms.communication.evidence-dir", "");
        if (!directory.isBlank()) {
            Path folder = Path.of(directory); Files.createDirectories(folder);
            json.writerWithDefaultPrettyPrinter().writeValue(folder.resolve("http-db-smtp-steps.json").toFile(), evidence);
        }
    }
}
