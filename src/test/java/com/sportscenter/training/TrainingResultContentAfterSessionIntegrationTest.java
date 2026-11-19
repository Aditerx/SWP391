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
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.concurrent.*;
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;

/** D15/RP-36, Session2.2/R2: loopback HTTP, real login/auth-me, committed synthetic data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:training_result_content_after;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false",
        "scms.ai.chat.enabled=false"
})
@Import(TrainingResultContentAfterSessionIntegrationTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingResultContentAfterSessionIntegrationTest {
    static class TestClock extends Clock {
        Instant current;
        ZoneId zone = SessionAttendancePolicy.ZONE;
        public ZoneId getZone() { return zone; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        public Instant instant() { return current; }
        void at(String time) { current = LocalDateTime.parse(time.contains("T") ? time : "2026-11-12T" + time).atZone(SessionAttendancePolicy.ZONE).toInstant(); }
    }
    @TestConfiguration static class Config {
        @Bean @Primary TestClock contentClock() { var clock = new TestClock(); clock.at("06:30:00"); return clock; }
    }
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url = System.getProperty("scms.result-content.pg-url", "");
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
    @Autowired javax.sql.DataSource dataSource;
    @SpyBean AuditService audit;
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
    private static final LocalDate DATE = LocalDate.of(2026, 11, 12);
    private static final LocalDateTime CHECK_IN = DATE.atTime(6, 1);

    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t -> work.run()); }
    @BeforeAll void actors() {
        tx(() -> {
            if (System.getProperty("scms.result-content.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
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
        var u = new User(); u.setEmail(name + "@content-after.test.invalid"); u.setFullName(name); u.setRole(role); u.setCenterId(1);
        u.setStatus("Active"); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u);
        if ("Coach".equals(role.getName())) { var c = new Coach(); c.setUser(u); em.persist(c); }
        if ("Member".equals(role.getName())) { var m = new Member(); m.setUser(u); m.setJoinDate(DATE.minusDays(10)); em.persist(m); }
        if ("Receptionist".equals(role.getName())) { var r = new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    @BeforeEach void fixture() {
        clock.zone = SessionAttendancePolicy.ZONE; clock.at("08:00:00"); sequence++;
        tx(() -> {
            var r = em.find(Role.class, coachRole.getId()); r.getPermissions().clear(); r.getPermissions().add(record);
            for (User u : List.of(coach, coachB, member, memberB, outsider)) { var current = em.find(User.class, u.getId()); current.setStatus("Active"); current.setCenterId(1); }
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
    private int resultId() { return jdbc.queryForObject("SELECT result_id FROM training_results WHERE session_id=? AND member_id=?", Integer.class, session.getId(), member.getId()); }
    private HttpResponse<String> patch(HttpClient client, Object body) throws Exception {
        return send(client,"PATCH","/api/training-results/"+resultId()+"/content",body);
    }
    private List<Object> protectedState() {
        return List.of(jdbc.queryForList("SELECT result_id,session_id,member_id,coach_id,attendance_status,recorded_at FROM training_results WHERE session_id=? ORDER BY result_id",session.getId()),
                jdbc.queryForList("SELECT * FROM attendances WHERE session_id=? ORDER BY attendance_id",session.getId()),
                jdbc.queryForList("SELECT * FROM sessions WHERE session_id=?",session.getId()),
                jdbc.queryForList("SELECT * FROM classes WHERE class_id=?",course.getId()),
                jdbc.queryForList("SELECT * FROM class_enrollments WHERE class_id=? ORDER BY enrollment_id",course.getId()),
                jdbc.queryForList("SELECT * FROM invoices WHERE member_id=? ORDER BY invoice_id",member.getId()),
                jdbc.queryForList("SELECT * FROM member_packages WHERE member_id=? ORDER BY subscription_id",member.getId()),
                jdbc.queryForList("SELECT * FROM notifications ORDER BY notification_id"));
    }
    private List<Object> snapshot() {
        return List.of(protectedState(),jdbc.queryForList("SELECT * FROM training_results WHERE session_id=? ORDER BY result_id",session.getId()),
                jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"));
    }
    private int auditCount() { return jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",Integer.class,resultId()); }
    private void readback(String content) throws Exception {
        var response=send(login(member),"GET","/api/training-results?sessionId="+session.getId(),null);
        assertEquals(200,response.statusCode()); var rows=json.readTree(response.body()); assertEquals(1,rows.size());
        assertEquals(content,rows.get(0).path("content").asText()); assertEquals(resultId(),rows.get(0).path("resultId").asInt());
        assertEquals(content,jdbc.queryForObject("SELECT content FROM training_results WHERE result_id=?",String.class,resultId()));
    }
    private void denied(HttpClient client, int status, String marker) throws Exception {
        var before=snapshot(); var response=patch(client,Map.of("content","Must not persist"));
        assertEquals(status,response.statusCode(),response.body()); assertTrue(response.body().contains(marker),response.body());
        assertEquals(before,snapshot(),"Denied mutation/audit must not commit");
    }
    @ParameterizedTest @ValueSource(strings={"07:30:00","23:59:59.999999"})
    void endInclusiveAndLastMicrosecondChangeOnlyContentAndMemberCanRead(String time) throws Exception {
        clock.at(time); var client=login(coach); var before=protectedState(); int count=auditCount();
        var response=patch(client,Map.of("content","Bổ sung kỹ thuật "+time)); assertEquals(200,response.statusCode(),response.body());
        var row=json.readTree(response.body()); assertEquals(resultId(),row.path("resultId").asInt());
        assertEquals(coach.getId(),row.path("coachId").asInt()); assertEquals("Present",row.path("attendanceStatus").asText());
        assertEquals("Original note",row.path("note").asText()); assertEquals(CHECK_IN,LocalDateTime.parse(row.path("recordedAt").asText()));
        assertEquals(before,protectedState()); assertEquals(count+1,auditCount()); readback("Bổ sung kỹ thuật "+time);
        assertEquals(coach.getId(),jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",Integer.class,resultId()));
        assertEquals(1,jdbc.queryForObject("SELECT center_id FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",Integer.class,resultId()));
        assertEquals("BUSINESS",jdbc.queryForObject("SELECT audit_category FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",String.class,resultId()));
        assertEquals("TARGET_AT_EVENT",jdbc.queryForObject("SELECT scope_source FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",String.class,resultId()));
        String detail=jdbc.queryForObject("SELECT detail FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",String.class,resultId());
        assertTrue(detail.contains("sessionId="+session.getId())); assertTrue(detail.contains("memberId="+member.getId())); assertTrue(detail.contains("UPDATED"));
        assertFalse(detail.contains("Bổ sung kỹ thuật"));
        assertEquals(SessionAttendancePolicy.now(clock),jdbc.queryForObject("SELECT l.timestamp FROM system_logs l WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",java.sql.Timestamp.class,resultId()).toLocalDateTime());
    }
    @Test void nullOmittedRejectedButEmptyAndWhitespaceRemainExplicitValues() throws Exception {
        var client=login(coach); var body=new LinkedHashMap<String,Object>();
        for(boolean explicitNull:List.of(false,true)) {
            if(explicitNull)body.put("content",null); var before=snapshot(); var response=patch(client,body);
            assertEquals(400,response.statusCode()); assertTrue(response.body().contains("content")); assertEquals(before,snapshot());
        }
        var before=protectedState(); assertEquals(200,patch(client,Map.of("content","")).statusCode()); readback("");
        assertEquals("sessionId="+session.getId()+"; memberId="+member.getId()+"; change=CLEARED",jdbc.queryForObject("SELECT detail FROM system_logs WHERE action='UPDATE_RESULT_CONTENT' AND target_id=?",String.class,resultId()));
        assertEquals(200,patch(client,Map.of("content"," \t ")).statusCode()); readback(" \t "); assertEquals(before,protectedState());
    }
    @Test void noOpAndRetryDoNotLogOrChangeOtherFields() throws Exception {
        var client=login(coach); var before=snapshot(); assertEquals(200,patch(client,Map.of("content",ORIGINAL)).statusCode()); assertEquals(before,snapshot());
        assertEquals(200,patch(client,Map.of("content","Once")).statusCode()); before=snapshot();
        assertEquals(200,patch(client,Map.of("content","Once")).statusCode()); assertEquals(before,snapshot()); assertEquals(1,auditCount());
    }
    @ParameterizedTest @ValueSource(strings={"05:59:59","07:29:59.999999"})
    void beforeEndIsRejected(String time) throws Exception { clock.at(time); denied(login(coach),400,"RESULT_CONTENT_NOT_OPEN"); }
    @ParameterizedTest @ValueSource(strings={"2026-11-13T00:00:00","2026-11-13T07:29:59"})
    void cutoffIsExclusiveAndIsNot24HoursAfterEnd(String time) throws Exception { clock.at(time); denied(login(coach),403,"RESULT_CONTENT_DEADLINE_PASSED"); }
    @ParameterizedTest @ValueSource(strings={"CenterManager","Receptionist","Admin","Member"})
    void nonCoachCannotUseExtraAuthorityToWrite(String role) throws Exception { denied(login(actors.get(role)),403,""); }
    @Test void coachNeedsRecordResultEvenWithManageClassesAndPermissionRefresh() throws Exception {
        var client=login(coach); tx(()->{var r=em.find(Role.class,coachRole.getId());r.getPermissions().clear();r.getPermissions().add(manage);});
        denied(client,403,"");
    }
    @Test void inactiveAndCrossCenterCoachCannotMutate() throws Exception {
        var client=login(coach); jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",coach.getId()); denied(client,403,"");
        jdbc.update("UPDATE users SET status='Active',center_id=2 WHERE user_id=?",coach.getId()); denied(login(coach),403,"");
    }
    @Test void wrongCoachAndClientIdentifiersCannotSelectAnotherTarget() throws Exception {
        denied(login(coachB),403,"RESULT_CONTENT_FORBIDDEN"); var client=login(coach); var before=protectedState();
        var body=Map.of("content","Trusted target only","memberId",memberB.getId(),"sessionId",Integer.MAX_VALUE,"coachId",coachB.getId(),"attendanceStatus","Absent","note","Overwrite note","role","Admin");
        assertEquals(200,patch(client,body).statusCode()); assertEquals(before,protectedState()); readback("Trusted target only");
        assertEquals(ORIGINAL,jdbc.queryForObject("SELECT content FROM training_results WHERE session_id=? AND member_id=?",String.class,session.getId(),memberB.getId()));
    }
    @Test void reassignmentDoesNotGiveOldOrNewCoachWriteOverOldAuthor() throws Exception {
        jdbc.update("UPDATE sessions SET coach_id=? WHERE session_id=?",coachB.getId(),session.getId());
        denied(login(coach),403,"RESULT_CONTENT_FORBIDDEN"); denied(login(coachB),403,"RESULT_CONTENT_FORBIDDEN");
        var own=send(login(coach),"GET","/api/training-results?sessionId="+session.getId(),null); assertEquals(200,own.statusCode());assertEquals(2,json.readTree(own.body()).size());
    }
    @Test void nullSessionCoachFallsBackToClassCoachWithoutChangingAuthor() throws Exception {
        jdbc.update("UPDATE sessions SET coach_id=null WHERE session_id=?",session.getId());
        jdbc.update("UPDATE classes SET coach_id=? WHERE class_id=?",coach.getId(),course.getId());
        var before=protectedState(); assertEquals(200,patch(login(coach),Map.of("content","Fallback assignment")).statusCode()); assertEquals(before,protectedState());
    }
    @Test void missingResultNeverCreatesAnythingAndMissingAttendanceStaysMissing() throws Exception {
        var client=login(coach); var before=snapshot(); var response=send(client,"PATCH","/api/training-results/2147483647/content",Map.of("content","Missing"));
        assertEquals(404,response.statusCode()); assertEquals(before,snapshot());
        jdbc.update("DELETE FROM attendances WHERE session_id=? AND member_id=?",session.getId(),member.getId()); before=protectedState();
        response=patch(client,Map.of("content","No attendance created")); assertEquals(200,response.statusCode(),response.body()); assertTrue(json.readTree(response.body()).path("note").isNull());
        assertEquals(before,protectedState()); readback("No attendance created");
    }
    @ParameterizedTest @ValueSource(strings={"Cancelled","Invalidated","Superseded","NotStarted"})
    void sessionsNotHeldAndSupersededAttendanceAreRejected(String variant) throws Exception {
        String code;
        switch(variant) {
            case "Cancelled" -> {jdbc.update("UPDATE sessions SET status='Cancelled' WHERE session_id=?",session.getId());code="SESSION_NOT_HELD";}
            case "Invalidated" -> {jdbc.update("UPDATE sessions SET center_invalidated=true WHERE session_id=?",session.getId());code="SESSION_NOT_HELD";}
            case "Superseded" -> {jdbc.update("UPDATE attendances SET superseded_at=? WHERE session_id=? AND member_id=?",CHECK_IN,session.getId(),member.getId());code="ATTENDANCE_SUPERSEDED";}
            default -> {jdbc.update("UPDATE classes SET min_capacity=1,status='Open' WHERE class_id=?",course.getId());code="NORMAL_NOT_STARTED";}
        }
        denied(login(coach),400,code);
    }
    @Test void completedFinalizedSessionAndEnrollmentKeepHistoricalEligibility() throws Exception {
        jdbc.update("UPDATE sessions SET status='Completed',attendance_finalized=true WHERE session_id=?",session.getId());
        jdbc.update("UPDATE classes SET min_capacity=1,status='Closed' WHERE class_id=?",course.getId());
        jdbc.update("UPDATE class_enrollments SET status='Completed' WHERE class_id=?",course.getId());
        var before=protectedState();assertEquals(200,patch(login(coach),Map.of("content","Completed own session")).statusCode());assertEquals(before,protectedState());readback("Completed own session");
    }
    @ParameterizedTest @ValueSource(strings={"Cancelled","Pending","NotPaid","Reconciliation","Reversed","EnrolledLate"})
    void invalidSessionMembershipOrPaymentDoesNotBecomeEligibleFromExistingResult(String variant) throws Exception {
        if(List.of("Cancelled","Pending").contains(variant))jdbc.update("UPDATE class_enrollments SET status=? WHERE class_id=? AND member_id=?",variant,course.getId(),member.getId());
        if("NotPaid".equals(variant))jdbc.update("UPDATE invoices SET payment_status='Pending' WHERE class_id=? AND member_id=?",course.getId(),member.getId());
        if("Reconciliation".equals(variant))jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE class_id=? AND member_id=?",course.getId(),member.getId());
        if("Reversed".equals(variant))jdbc.update("UPDATE invoices SET cash_confirmation_reversed=true WHERE class_id=? AND member_id=?",course.getId(),member.getId());
        if("EnrolledLate".equals(variant))jdbc.update("UPDATE class_enrollments SET enrolled_at=? WHERE class_id=? AND member_id=?",DATE.atTime(8,0),course.getId(),member.getId());
        denied(login(coach),400,"NOT_YOUR_CLASS");
    }
    @ParameterizedTest @ValueSource(strings={"Completed","Expired"})
    void advancedConsumedEntitlementIsCheckedAtSessionNotCurrentTime(String status) throws Exception {
        advanced(status);var before=protectedState(); assertEquals(200,patch(login(coach),Map.of("content","Advanced history")).statusCode());assertEquals(before,protectedState());readback("Advanced history");
    }
    private void advanced(String status) {
        tx(()->{
            var c=em.find(SportsClass.class,course.getId());c.setClassType("Advanced");c.setMaxCapacity(1);
            var sub=new com.sportscenter.membership.MemberPackage();sub.setMember(member);sub.setSubject(subject);sub.setPrimaryCoach(coach);sub.setPurchaseAmount(new BigDecimal("3000000"));
            sub.setActivatedAt(DATE.minusDays(40).atTime(8,0));sub.setExpiresAt(DATE.atTime(7,30));sub.setStartDate(DATE.minusDays(40));sub.setEndDate(DATE);sub.setStatus(status);sub.setRemainingCredit(0);em.persist(sub);
            var s=em.find(Session.class,session.getId());s.setBookedMember(member);s.setMemberPackage(sub);
            var i=new Invoice();i.setMember(member);i.setSubscription(sub);i.setInvoiceCode("AFTER-ADV-"+sequence);i.setAmount(new BigDecimal("3000000"));i.setPaymentStatus("Paid");i.setPaymentMethod("Cash");i.setReceptionist(actors.get("Receptionist"));i.setPaymentDate(DATE.minusDays(40).atTime(8,0));i.setCreatedAt(DATE.minusDays(40).atTime(8,0));em.persist(i);
        });
    }
    @Test void advancedWrongBookedMemberExpiredAtSessionAndCancelledEntitlementAreRejected() throws Exception {
        advanced("Completed");var client=login(coach);
        jdbc.update("UPDATE sessions SET booked_member_id=? WHERE session_id=?",memberB.getId(),session.getId());denied(client,400,"NOT_YOUR_CLASS");
        jdbc.update("UPDATE sessions SET booked_member_id=? WHERE session_id=?",member.getId(),session.getId());
        jdbc.update("UPDATE member_packages SET expires_at=? WHERE subscription_id=(SELECT subscription_id FROM sessions WHERE session_id=?)",DATE.atTime(7,29),session.getId());denied(client,400,"NOT_YOUR_CLASS");
        jdbc.update("UPDATE member_packages SET expires_at=?,status='Cancelled' WHERE subscription_id=(SELECT subscription_id FROM sessions WHERE session_id=?)",DATE.atTime(7,30),session.getId());denied(client,400,"NOT_YOUR_CLASS");
    }
    @Test void auditFailureRollsBackResultAndAuditTogether() throws Exception {
        var client=login(coach);var before=snapshot();
        AuditService target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit);
        doAnswer(i->{i.callRealMethod();throw new BusinessException("AUDIT_TEST_FAILURE: Synthetic rollback after audit insert");})
                .when(target).log(anyInt(),eq("UPDATE_RESULT_CONTENT"),eq("training_results"),anyInt(),anyString(),any(LocalDateTime.class));
        var response=patch(client,Map.of("content","Must roll back"));assertEquals(400,response.statusCode(),response.body());assertTrue(response.body().contains("AUDIT_TEST_FAILURE"));assertEquals(before,snapshot());
    }
    @Test void concurrentIdenticalWritesCommitOneChangeAndOneAudit() throws Exception {
        var client=login(coach);var before=protectedState();var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try(var blocker=dataSource.getConnection()) {
            blocker.setAutoCommit(false);blocker.createStatement().executeQuery("SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE").close();
            var futures=new ArrayList<Future<HttpResponse<String>>>();for(int n=0;n<2;n++)futures.add(pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return patch(client,Map.of("content","Concurrent once"));}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();int waiting=awaitBlocked();blocker.commit();assertEquals(2,waiting,"Both HTTP mutations must reach the controlled center lock");
            for(var f:futures){var response=f.get(30,TimeUnit.SECONDS);assertEquals(200,response.statusCode(),response.body());}
            assertEquals(1,auditCount());assertEquals(before,protectedState());readback("Concurrent once");
        } finally {start.countDown();pool.shutdownNow();}
    }
    @Test void attendanceCorrectionAndContentWriteSerializeWithoutLostFields() throws Exception {
        var client=login(coach);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try(var blocker=dataSource.getConnection()) {
            blocker.setAutoCommit(false);blocker.createStatement().executeQuery("SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE").close();
            var a=pool.submit(()->{assertTrue(start.await(10,TimeUnit.SECONDS));return patch(client,Map.of("content","Content with correction"));});
            var b=pool.submit(()->{assertTrue(start.await(10,TimeUnit.SECONDS));return send(client,"POST","/api/attendances/correct",Map.of("sessionId",session.getId(),"memberId",member.getId(),"newState","Absent","reason","Synthetic review","note","Corrected note"));});
            start.countDown();int waiting=awaitBlocked();blocker.commit();assertEquals(2,waiting,"Correction and content must share the center lock");
            for(var f:List.of(a,b)){var response=f.get(30,TimeUnit.SECONDS);assertEquals(200,response.statusCode(),response.body());}
            readback("Content with correction");assertEquals(1,auditCount());
            assertEquals("Absent",jdbc.queryForObject("SELECT attendance_status FROM training_results WHERE result_id=?",String.class,resultId()));
            assertEquals("Corrected note",jdbc.queryForObject("SELECT note FROM attendances WHERE session_id=? AND member_id=?",String.class,session.getId(),member.getId()));
            assertEquals(CHECK_IN,jdbc.queryForObject("SELECT check_in_time FROM attendances WHERE session_id=? AND member_id=?",java.sql.Timestamp.class,session.getId(),member.getId()).toLocalDateTime());
        }finally{start.countDown();pool.shutdownNow();}
    }
    private int awaitBlocked() throws Exception {
        boolean h2=System.getProperty("scms.result-content.pg-url","").isBlank();
        String query=h2?"SELECT count(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL"
                :"SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'";
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);int waiting=0;
        while(System.nanoTime()<until){waiting=jdbc.queryForObject(query,Integer.class);if(waiting>=2)break;Thread.sleep(20);}
        return waiting;
    }
    @Test void vietnamWindowDoesNotDependOnInjectedClockZone() throws Exception {
        clock.at("23:59:59.999999");clock.zone=ZoneOffset.UTC;assertEquals(200,patch(login(coach),Map.of("content","VN time")).statusCode());
        clock.at("2026-11-13T00:00:00");denied(login(coach),403,"RESULT_CONTENT_DEADLINE_PASSED");
    }
    @Test void anonymousRequestIsOnlyAuthenticationGate() throws Exception {
        var before=snapshot();var response=patch(HttpClient.newHttpClient(),Map.of("content","Anonymous"));assertEquals(401,response.statusCode());assertEquals(before,snapshot());
    }
    @Test void openApiPublishesOneContentFieldAndKeepsExistingPostAndResponse() throws Exception {
        var response=send(login(coach),"GET","/v3/api-docs",null);assertEquals(200,response.statusCode());var spec=json.readTree(response.body());
        var operation=spec.path("paths").path("/api/training-results/{resultId}/content").path("patch");assertFalse(operation.isMissingNode());
        assertTrue(operation.path("description").asText().contains("midnight"));assertTrue(spec.path("paths").path("/api/training-results").has("post"));
        var schemas=spec.path("components").path("schemas");var request=schemas.path("TrainingResultContentRequest");
        assertEquals(1,request.path("properties").size());assertEquals("content",request.path("required").get(0).asText());assertEquals("string",request.path("properties").path("content").path("type").asText());
        assertEquals("#/components/schemas/TrainingResultResponse",operation.path("responses").path("200").path("content").path("*/*").path("schema").path("$ref").asText());
        for(String status:List.of("400","401","403","404"))assertTrue(operation.path("responses").has(status));
        String out=System.getProperty("scms.result-content.openapi","");
        if(!out.isBlank())json.writerWithDefaultPrettyPrinter().writeValue(java.nio.file.Path.of(out).toFile(),spec);
    }
}
