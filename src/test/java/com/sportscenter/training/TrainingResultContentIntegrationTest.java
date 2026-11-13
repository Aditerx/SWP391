package com.sportscenter.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.subject.Subject;
import com.sportscenter.user.*;
import com.sportscenter.user.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** D15/RP-36, Session 1.1: loopback HTTP, real login/auth-me, committed synthetic data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:training_content;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false",
        "scms.ai.chat.enabled=false"
})
@Import(TrainingResultContentIntegrationTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingResultContentIntegrationTest {
    static class TestClock extends Clock {
        Instant current;
        public ZoneId getZone() { return SessionAttendancePolicy.ZONE; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        public Instant instant() { return current; }
        void at(String time) { current = LocalDateTime.parse("2026-11-11T" + time).atZone(getZone()).toInstant(); }
    }
    @TestConfiguration static class Config {
        @Bean @Primary TestClock contentClock() { var clock = new TestClock(); clock.at("06:30:00"); return clock; }
    }
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url = System.getProperty("scms.content.pg-url", "");
        if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+"))
            throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> "postgres"); r.add("spring.datasource.password", () -> "scms-local-only");
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired TestClock clock;
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;

    private final Map<String, User> actors = new HashMap<>();
    private User coach, coachB, member, memberB, outsider;
    private Role coachRole;
    private Permission record, manage;
    private Subject subject;
    private Room room;
    private Session session;
    private SportsClass course;
    private int sequence;
    private static final String PASSWORD = "Content-local-synthetic-2026!";
    private static final String ORIGINAL = "Coach assessment to preserve";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 11);
    private static final LocalDateTime CHECK_IN = DATE.atTime(6, 1);

    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t -> work.run()); }
    @BeforeAll void actors() {
        tx(() -> {
            if (System.getProperty("scms.content.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Content local A'),(2,'Content local B') ON CONFLICT(center_id) DO NOTHING");
            record = permission("RECORD_RESULT"); manage = permission("MANAGE_CLASSES");
            coachRole = role("Coach", record);
            coach = user("coach-a", coachRole); coachB = user("coach-b", coachRole);
            var memberRole = role("Member", record);
            member = user("member-a", memberRole); memberB = user("member-b", memberRole); outsider = user("outsider", memberRole);
            actors.put("CenterManager", user("manager", role("CenterManager", manage)));
            actors.put("Receptionist", user("receptionist", role("Receptionist", record)));
            actors.put("Admin", user("admin", role("Admin", manage, record)));
            actors.put("Member", member);
            var subjects = em.createQuery("select s from Subject s where s.slug='cau-long'", Subject.class).getResultList();
            if (subjects.isEmpty()) {
                subject = new Subject(); subject.setName("Cầu lông"); subject.setSlug("cau-long");
                subject.setNormalPrice(new BigDecimal("900000")); subject.setAdvancedPrice(new BigDecimal("3000000")); em.persist(subject);
            } else subject = subjects.get(0);
            room = new Room(); room.setName("Content local court"); room.setCenterId(1); room.setCapacity(15); room.setStatus("Available"); em.persist(room);
        });
    }
    private Permission permission(String name) { var p = new Permission(); p.setName(name); em.persist(p); return p; }
    private Role role(String name, Permission... permissions) {
        var r = new Role(); r.setName(name); r.getPermissions().addAll(List.of(permissions)); em.persist(r); return r;
    }
    private User user(String name, Role role) {
        var u = new User(); u.setEmail(name + "@content.test.invalid"); u.setFullName(name); u.setRole(role); u.setCenterId(1);
        u.setStatus("Active"); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u);
        if ("Coach".equals(role.getName())) { var c = new Coach(); c.setUser(u); em.persist(c); }
        if ("Member".equals(role.getName())) { var m = new Member(); m.setUser(u); m.setJoinDate(DATE.minusDays(10)); em.persist(m); }
        if ("Receptionist".equals(role.getName())) { var r = new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    @BeforeEach void fixture() {
        clock.at("06:30:00"); sequence++;
        tx(() -> {
            var r = em.find(Role.class, coachRole.getId()); r.getPermissions().clear(); r.getPermissions().add(record);
            for (User u : List.of(coach, coachB)) { var current = em.find(User.class, u.getId()); current.setStatus("Active"); current.setCenterId(1); }
            course = new SportsClass(); course.setName("Content synthetic " + sequence); course.setClassType("Normal");
            course.setSubject(subject); course.setCoach(coachB); course.setCenterId(1); course.setMaxCapacity(15);
            course.setStatus("Ongoing"); course.setTuitionFee(new BigDecimal("900000")); em.persist(course);
            // Session assignment wins over class Coach, including substitutions.
            session = new Session(); session.setSportsClass(course); session.setCoach(coach); session.setRoom(room);
            session.setSessionDate(DATE); session.setStartTime(LocalTime.of(6,0)); session.setEndTime(LocalTime.of(7,30)); em.persist(session);
            for (User m : List.of(member, memberB)) {
                var e = new ClassEnrollment(); e.setMember(m); e.setSportsClass(course); e.setStatus("Registered"); e.setEnrolledAt(DATE.minusDays(1).atTime(8,0)); em.persist(e);
                var invoice = new Invoice(); invoice.setMember(m); invoice.setSportsClass(course); invoice.setAmount(new BigDecimal("900000"));
                invoice.setInvoiceCode("CONTENT-" + sequence + "-" + m.getId()); invoice.setPaymentStatus("Paid"); invoice.setPaymentMethod("Cash");
                invoice.setReceptionist(actors.get("Receptionist")); invoice.setPaymentDate(DATE.minusDays(1).atTime(8,0)); invoice.setCreatedAt(DATE.minusDays(1).atTime(8,0)); em.persist(invoice);
                var result = new TrainingResult(); result.setSession(session); result.setMember(m); result.setCoach(coach);
                result.setContent(ORIGINAL); result.setAttendanceStatus("Present"); result.setRecordedAt(CHECK_IN); em.persist(result);
                var attendance = new Attendance(); attendance.setSession(session); attendance.setMember(m); attendance.setRecordedBy(coach);
                attendance.setState("Present"); attendance.setCheckInTime(CHECK_IN); attendance.setNote("Original note"); em.persist(attendance);
            }
        });
    }
    private HttpResponse<String> send(HttpClient client, String method, String path, Object body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(20));
        if (body == null) b.GET(); else b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient login(User actor) throws Exception {
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var response = send(client, "POST", "/api/auth/login", Map.of("email", actor.getEmail(), "password", PASSWORD));
        assertEquals(200, response.statusCode(), "Synthetic login");
        var me = send(client, "GET", "/api/auth/me", null); assertEquals(200, me.statusCode());
        assertEquals(actor.getId(), json.readTree(me.body()).path("userId").asInt());
        return client;
    }
    private Map<String,Object> item(User target) {
        var b = new LinkedHashMap<String,Object>(); b.put("memberId", target.getId()); b.put("attendanceStatus", "Absent"); b.put("note", " Staff note "); return b;
    }
    private HttpResponse<String> write(HttpClient client, boolean bulk, Map<String,Object> item) throws Exception {
        var body = new LinkedHashMap<String,Object>(); body.put("sessionId", session.getId()); body.put("coachId", coachB.getId());
        body.put("actor", coach.getId()); body.put("role", "Coach"); // Unknown client fields cannot confer privilege.
        if (bulk) body.put("items", List.of(item)); else body.putAll(item);
        return send(client, "POST", "/api/training-results" + (bulk ? "/bulk" : ""), body);
    }
    private List<Object> snapshot() {
        // Independent connections read only committed state; include all business writes and audit.
        return List.of(jdbc.queryForList("SELECT * FROM training_results WHERE session_id=? ORDER BY result_id", session.getId()),
                jdbc.queryForList("SELECT * FROM attendances WHERE session_id=? ORDER BY attendance_id", session.getId()),
                jdbc.queryForList("SELECT * FROM sessions WHERE session_id=?", session.getId()),
                jdbc.queryForList("SELECT * FROM class_enrollments WHERE class_id=? ORDER BY enrollment_id", course.getId()),
                jdbc.queryForList("SELECT * FROM invoices WHERE class_id=? ORDER BY invoice_id", course.getId()),
                jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"));
    }
    private void readback(String expected, User author) throws Exception {
        // RECORD_RESULT alone does not grant staff GET access; verify using the owning Member.
        var response = send(login(member), "GET", "/api/training-results?sessionId=" + session.getId() + "&memberId=" + member.getId(), null);
        assertEquals(200, response.statusCode()); JsonNode row = json.readTree(response.body()).get(0);
        assertEquals(expected, row.path("content").asText()); assertEquals(coach.getId(), row.path("coachId").asInt());
        assertEquals(expected, jdbc.queryForObject("SELECT content FROM training_results WHERE session_id=? AND member_id=?", String.class, session.getId(), member.getId()));
        assertEquals(CHECK_IN, jdbc.queryForObject("SELECT check_in_time FROM attendances WHERE session_id=? AND member_id=?", java.sql.Timestamp.class, session.getId(), member.getId()).toLocalDateTime());
        assertEquals(author.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='RECORD_RESULT' ORDER BY log_id DESC LIMIT 1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT center_id FROM system_logs WHERE action='RECORD_RESULT' ORDER BY log_id DESC LIMIT 1", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND amount=900000 AND payment_status='Paid'", Integer.class, course.getId()));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Registered'", Integer.class, course.getId()));
        assertEquals("Scheduled", jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?", String.class, session.getId()));
    }

    @ParameterizedTest @ValueSource(strings={"CenterManager","Receptionist","Admin","Member"})
    void nonCoachCannotWriteOrEraseContent(String role) throws Exception {
        var actor = login(actors.get(role));
        for (boolean bulk : List.of(false, true)) for (String content : List.of("", " \t ", "Unauthorized assessment")) {
            var before = snapshot(); var item = item(member); item.put("content", content);
            assertEquals(403, write(actor, bulk, item).statusCode(), role + ", bulk=" + bulk + ", content length=" + content.length());
            assertEquals(before, snapshot(), "Denied request must not persist attendance, content, note or audit");
        }
    }
    @ParameterizedTest @ValueSource(strings={"CenterManager","Receptionist","Admin"})
    void staffOmittedOrNullContentPreservesAssessment(String role) throws Exception {
        var actor = login(actors.get(role));
        for (boolean bulk : List.of(false, true)) for (boolean explicitNull : List.of(false, true)) {
            var item = item(member); if (explicitNull) item.put("content", null);
            var response = write(actor, bulk, item); assertEquals(200, response.statusCode());
            var row = json.readTree(response.body()); if (bulk) row = row.get(0);
            assertEquals(ORIGINAL, row.path("content").asText()); assertEquals("Absent", row.path("attendanceStatus").asText()); assertEquals("Staff note", row.path("note").asText());
            readback(ORIGINAL, actors.get(role));
        }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void assignedCoachCanWriteIncludingEmptyContentAndIgnoresClientCoach(boolean bulk) throws Exception {
        var actor = login(coach);
        for (String content : List.of("Updated technique", "", "   ")) {
            var item = item(member); item.put("content", content);
            assertEquals(200, write(actor, bulk, item).statusCode()); readback(content, coach);
        }
        var owner = login(member); var own = send(owner, "GET", "/api/training-results?sessionId=" + session.getId(), null);
        assertEquals(200, own.statusCode()); assertEquals(1, json.readTree(own.body()).size()); assertEquals("   ", json.readTree(own.body()).get(0).path("content").asText());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void coachWithManageClassesStillNeedsRecordResultForContent(boolean bulk) throws Exception {
        tx(() -> { var r = em.find(Role.class, coachRole.getId()); r.getPermissions().clear(); r.getPermissions().add(manage); });
        var actor = login(coach);
        assertEquals(200, write(actor, bulk, item(member)).statusCode(), "Attendance authority is retained");
        readback(ORIGINAL, coach);
        for (String content : List.of("", "   ", "Assessment")) {
            var before = snapshot(); var item = item(member); item.put("content", content);
            assertEquals(403, write(actor, bulk, item).statusCode()); assertEquals(before, snapshot());
        }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void otherCoachInSameCenterCannotUseClassAssignmentOrSpoofActor(boolean bulk) throws Exception {
        var actor = login(coachB); var before = snapshot(); var item = item(member); item.put("content", "Wrong coach");
        var response = write(actor, bulk, item); assertEquals(400, response.statusCode()); assertTrue(response.body().contains("NOT_YOUR_CLASS")); assertEquals(before, snapshot());
    }
    @Test void bulkDeniedSecondItemRollsBackEarlierAttendanceAndNote() throws Exception {
        var actor = login(actors.get("CenterManager")); var before = snapshot(); var forbidden = item(memberB); forbidden.put("content", "");
        var response = send(actor, "POST", "/api/training-results/bulk", Map.of("sessionId", session.getId(), "items", List.of(item(member), forbidden)));
        assertEquals(403, response.statusCode()); assertEquals(before, snapshot());
    }
    @Test void bulkCannotHideUnauthorizedContentInSkippedMemberItems() throws Exception {
        var actor = login(actors.get("CenterManager"));
        for (boolean missingId : List.of(false, true)) {
            var forbidden = item(outsider); if (missingId) forbidden.remove("memberId"); else forbidden.put("memberId", Integer.MAX_VALUE);
            forbidden.put("content", "   "); var before = snapshot();
            var response = send(actor, "POST", "/api/training-results/bulk", Map.of("sessionId", session.getId(), "items", List.of(item(member), forbidden)));
            assertEquals(403, response.statusCode()); assertEquals(before, snapshot());
        }
    }
    @Test void bulkCoachFailureRollsBackEarlierAssessmentAttendanceAndAudit() throws Exception {
        var actor = login(coach); var before = snapshot(); var first = item(member); first.put("content", "Must rollback");
        var invalid = item(memberB); invalid.put("attendanceStatus", "Late");
        var response = send(actor, "POST", "/api/training-results/bulk", Map.of("sessionId", session.getId(), "items", List.of(first, invalid)));
        assertEquals(400, response.statusCode()); assertEquals(before, snapshot());
    }
    @Test void windowBoundariesAndMissingResultStayUnchanged() throws Exception {
        var actor = login(coach); var item = item(member); item.put("content", "At start");
        clock.at("05:59:59"); var before = snapshot(); assertEquals(400, write(actor,false,item).statusCode()); assertEquals(before,snapshot());
        clock.at("06:00:00"); assertEquals(200,write(actor,false,item).statusCode());
        jdbc.update("DELETE FROM training_results WHERE session_id=? AND member_id=?",session.getId(),memberB.getId());
        jdbc.update("DELETE FROM attendances WHERE session_id=? AND member_id=?",session.getId(),memberB.getId());
        clock.at("07:30:00"); before = snapshot();
        for (boolean bulk : List.of(false,true)) { assertEquals(400,write(actor,bulk,item(memberB)).statusCode()); assertEquals(before,snapshot()); }
    }
    @Test void inactiveAndWrongCenterCoachAndIneligibleMemberAreStillDenied() throws Exception {
        var actor = login(coach); var item = item(member); item.put("content", "Denied");
        jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",coach.getId()); var before = snapshot();
        assertEquals(403,write(actor,false,item).statusCode()); assertEquals(before,snapshot());
        jdbc.update("UPDATE users SET status='Active',center_id=2 WHERE user_id=?",coach.getId()); before=snapshot();
        assertEquals(403,write(actor,true,item).statusCode()); assertEquals(before,snapshot());
        jdbc.update("UPDATE users SET center_id=1 WHERE user_id=?",coach.getId()); before=snapshot();
        var unknown = item(outsider); unknown.put("content","Denied"); assertEquals(400,write(actor,false,unknown).statusCode()); assertEquals(before,snapshot());
    }
}
