package com.sportscenter.training;

import com.fasterxml.jackson.databind.*;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.*;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.*;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.*;
import com.sportscenter.user.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/** D15 Owner09/11. Real login/auth-me, committed fixtures and DB readback; no .env/cloud. */
@SpringBootTest(properties={"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:attendance_correction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false"})
@AutoConfigureMockMvc @Import(AttendanceCorrectionIntegrationTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceCorrectionIntegrationTest {
    public static class TestClock extends Clock {
        volatile Instant current;
        public ZoneId getZone() { return SessionAttendancePolicy.ZONE; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        public Instant instant() { return current; }
        void at(String local) { current = LocalDateTime.parse(local).atZone(getZone()).toInstant(); }
    }
    @TestConfiguration static class Config { @Bean @Primary TestClock attendanceClock() { return new TestClock(); } }
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url = System.getProperty("scms.attendance.pg-url", "");
        if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+")) throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url", () -> url); r.add("spring.datasource.username", () -> "postgres"); r.add("spring.datasource.password", () -> "scms-local-only");
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver"); r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired TestClock clock;
    @Autowired AttendanceFinalizationProcessor finalizer;
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.auth.EmailService email;
    private User coach, coachB, member, otherMember, manager, receptionist;
    private com.sportscenter.subject.Subject subject;
    private Room room;
    private Session session;
    private SportsClass course;
    private final String password = "Attendance-local-2026!";
    private int sequence;
    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t -> work.run()); }
    @BeforeAll void actors() {
        clock.at("2026-11-09T07:30:00");
        tx(() -> {
            if (System.getProperty("scms.attendance.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Attendance local') ON CONFLICT(center_id) DO NOTHING");
            Permission result = permission("RECORD_RESULT"), classes = permission("MANAGE_CLASSES"), register = permission("REGISTER_MEMBER");
            Role cr = role("Coach", result), mr = role("Member"), mgr = role("CenterManager", classes), rr = role("Receptionist", register, result);
            coach = user("coach-a", cr); coachB = user("coach-b", cr); member = user("member", mr); otherMember = user("other-member", mr);
            manager = user("manager", mgr); receptionist = user("receptionist", rr);
            var subjects = em.createQuery("select s from Subject s where s.slug='cau-long'", com.sportscenter.subject.Subject.class).getResultList();
            if (subjects.isEmpty()) {
                subject = new com.sportscenter.subject.Subject(); subject.setName("Cầu lông"); subject.setSlug("cau-long"); subject.setNormalPrice(new BigDecimal("900000")); subject.setAdvancedPrice(new BigDecimal("3000000")); em.persist(subject);
            } else subject = subjects.get(0);
            room = new Room(); room.setName("Attendance local court"); room.setCenterId(1); room.setCapacity(15); room.setStatus("Available"); em.persist(room);
        });
    }
    private Permission permission(String name) { var p = new Permission(); p.setName(name); em.persist(p); return p; }
    private Role role(String name, Permission... ps) { var r = new Role(); r.setName(name); r.getPermissions().addAll(List.of(ps)); em.persist(r); return r; }
    private User user(String name, Role role) {
        var u = new User(); u.setEmail(name + "@attendance.test.invalid"); u.setFullName(name); u.setRole(role); u.setCenterId(1); u.setStatus("Active"); u.setPasswordHash(passwords.encode(password)); em.persist(u);
        if ("Member".equals(role.getName())) { var m = new Member(); m.setUser(u); m.setJoinDate(LocalDate.of(2026,11,1)); em.persist(m); }
        if ("Coach".equals(role.getName())) { var c = new Coach(); c.setUser(u); em.persist(c); }
        if ("Receptionist".equals(role.getName())) { var r = new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    @BeforeEach void fixture() {
        clock.at("2026-11-09T07:30:00"); sequence++;
        tx(() -> {
            course = new SportsClass(); course.setName("Attendance " + sequence); course.setClassType("Normal"); course.setSubject(subject); course.setCoach(coach); course.setCenterId(1); course.setMaxCapacity(15); course.setStatus("Closed"); course.setTuitionFee(new BigDecimal("900000")); em.persist(course);
            session = new Session(); session.setSportsClass(course); session.setCoach(coach); session.setRoom(room); session.setSessionDate(LocalDate.of(2026,11,9)); session.setStartTime(LocalTime.of(6,0)); session.setEndTime(LocalTime.of(7,30)); em.persist(session);
            var e = new ClassEnrollment(); e.setMember(member); e.setSportsClass(course); e.setStatus("Registered"); e.setEnrolledAt(LocalDateTime.of(2026,11,1,8,0)); em.persist(e);
            paid(null);
        });
    }
    private void paid(MemberPackage sub) {
        var i = new Invoice(); i.setMember(member); i.setSportsClass(sub == null ? course : null); i.setSubscription(sub); i.setInvoiceCode("ATT-" + sequence + "-" + (sub == null ? "N" : "A")); i.setAmount(new BigDecimal("900000")); i.setPaymentStatus("Paid"); i.setPaymentMethod("Cash"); i.setReceptionist(receptionist); i.setPaymentDate(LocalDateTime.of(2026,11,1,8,0)); i.setCreatedAt(LocalDateTime.of(2026,11,1,8,0)); em.persist(i);
    }
    private MockHttpSession login(User actor) throws Exception {
        var s = (MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email", actor.getEmail(), "password", password))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(actor.getId())); return s;
    }
    private Map<String,Object> target(String state) { var b = new LinkedHashMap<String,Object>(); b.put("sessionId",session.getId()); b.put("memberId",member.getId()); b.put("newState",state); b.put("reason","Internal reconciliation"); return b; }
    private ResultActions correct(MockHttpSession actor, Map<String,Object> body) throws Exception { return mvc.perform(post("/api/attendances/correct").session(actor).contentType("application/json").content(json.writeValueAsString(body))); }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + (table.equals("system_logs") ? "target_entity='attendances' AND action='ATTENDANCE_CORRECTION' AND target_id IN (SELECT attendance_id FROM attendances WHERE session_id=?)" : "session_id=?"), Integer.class, session.getId()); }
    private String stored(String column) { return jdbc.queryForObject("SELECT " + column + " FROM attendances WHERE session_id=? AND superseded_at IS NULL", String.class, session.getId()); }
    private int notifications() { return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?", Integer.class, "NO_SHOW_RECORDED:" + session.getId()); }

    @ParameterizedTest @ValueSource(strings={"2026-11-09T07:30:00","2026-11-09T23:59:59.999"})
    void coachCreatesAtEndAndJustBeforeMidnight(String time) throws Exception {
        clock.at(time); var actor = login(coach); var body = target("Present"); body.put("note","  Good participation  ");
        correct(actor,body).andExpect(status().isOk()).andExpect(jsonPath("$.note").value("Good participation")).andExpect(jsonPath("$.correctionAllowed").value(true))
                .andExpect(jsonPath("$.correctionDeadline").value("2026-11-10T00:00:00+07:00")).andExpect(jsonPath("$.checkInTime").isEmpty());
        assertEquals("Present", stored("state")); assertNull(stored("check_in_time")); assertEquals(1,count("attendances"));
        correct(actor,body).andExpect(status().isOk()); assertEquals(1,count("system_logs"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM training_results WHERE session_id=?",Integer.class,session.getId()));
        mvc.perform(get("/api/sessions/" + session.getId()).session(actor)).andExpect(status().isOk()).andExpect(jsonPath("$.correctionAllowed").value(true));
    }
    @Test void scopeCheckedBeforeTimeAndManagerHasNoCoachDeadline() throws Exception {
        var a = login(coach); var b = login(coachB); var m = login(manager); clock.at("2026-11-10T00:00:00");
        correct(a,target("Present")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ATTENDANCE_CORRECTION_DEADLINE_PASSED"));
        correct(b,target("Present")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ATTENDANCE_CORRECTION_FORBIDDEN"));
        correct(m,target("Present")).andExpect(status().isOk()).andExpect(jsonPath("$.correctionDeadline").isEmpty());
        assertEquals(1,count("attendances"));
    }
    @Test void manualClosesAtEndAndCorrectionDoesNotOpenBeforeEnd() throws Exception {
        var a=login(coach); clock.at("2026-11-09T07:29:59.999");
        correct(a,target("Present")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("USE_SESSION_ATTENDANCE"));
        Map<String,Object> b = new LinkedHashMap<>(Map.of("sessionId",session.getId(),"memberId",member.getId(),"coachId",coachB.getId(),"attendanceStatus","Present","content","Original professional content","note","During class"));
        mvc.perform(post("/api/training-results").session(a).contentType("application/json").content(json.writeValueAsString(b))).andExpect(status().isOk()).andExpect(jsonPath("$.note").value("During class"));
        b.remove("content"); b.put("note","Updated in class");
        mvc.perform(post("/api/training-results").session(a).contentType("application/json").content(json.writeValueAsString(b))).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Original professional content"));
        clock.at("2026-11-09T07:30:00");
        mvc.perform(post("/api/training-results").session(a).contentType("application/json").content(json.writeValueAsString(b))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ATTENDANCE_WINDOW_CLOSED"));
        var original=stored("check_in_time"); var edit=target("Absent"); edit.put("note","After class"); correct(a,edit).andExpect(status().isOk());
        assertEquals(original,stored("check_in_time"));
        assertEquals("Original professional content",jdbc.queryForObject("SELECT content FROM training_results WHERE session_id=?",String.class,session.getId()));
        assertEquals("Absent",jdbc.queryForObject("SELECT attendance_status FROM training_results WHERE session_id=?",String.class,session.getId()));
        assertEquals(coach.getId(),jdbc.queryForObject("SELECT coach_id FROM training_results WHERE session_id=?",Integer.class,session.getId()));
    }
    @Test void noteOmitNullClearAndMemberReadbackWithInternalAudit() throws Exception {
        var a=login(coach); var body=target("Absent"); body.put("note","Public note"); correct(a,body).andExpect(status().isOk());
        body.remove("note"); correct(a,body).andExpect(status().isOk()).andExpect(jsonPath("$.note").value("Public note"));
        body.put("note",null); correct(a,body).andExpect(status().isOk()).andExpect(jsonPath("$.note").value("Public note"));
        var memberSession=login(member);
        mvc.perform(get("/api/attendances").param("sessionId",session.getId().toString()).session(memberSession)).andExpect(status().isOk()).andExpect(jsonPath("$[0].note").value("Public note"))
                .andExpect(jsonPath("$[0].correctionAllowed").value(false)).andExpect(jsonPath("$[0].reason").doesNotExist());
        mvc.perform(get("/api/attendances").param("memberId",otherMember.getId().toString()).session(memberSession)).andExpect(status().isForbidden());
        int id=jdbc.queryForObject("SELECT attendance_id FROM attendances WHERE session_id=?",Integer.class,session.getId());
        body.remove("sessionId"); body.remove("memberId"); body.put("attendanceId",id); body.put("note","  "); correct(a,body).andExpect(status().isOk()).andExpect(jsonPath("$.note").isEmpty());
        assertNull(stored("note")); assertEquals(2,count("system_logs"));
        assertTrue(jdbc.queryForList("SELECT detail FROM system_logs WHERE action='ATTENDANCE_CORRECTION' AND target_id=?",String.class,id).stream().anyMatch(s -> s.contains("Public note") && s.contains("Internal reconciliation") && s.contains("\"after\":null")));
    }
    @Test void validationIsAtomicAndAcceptsTrimmedLimits() throws Exception {
        var a=login(coach); var b=target("Late"); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ATTENDANCE_STATE"));
        b=target("Present"); b.put("note","x".repeat(1001)); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOTE_TOO_LONG"));
        b=target("Present"); b.put("reason"," "); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        b.put("reason","x".repeat(1001)); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REASON_TOO_LONG")); assertEquals(0,count("attendances"));
        b.put("note"," " + "n".repeat(1000) + " "); b.put("reason"," " + "r".repeat(1000) + " "); correct(a,b).andExpect(status().isOk()); assertEquals(1000,stored("note").length());
    }
    @Test void rejectsAmbiguousIncompleteAndIneligibleTargets() throws Exception {
        var a=login(coach);
        for (Map<String,Object> b : List.<Map<String,Object>>of(new LinkedHashMap<>(Map.of("newState","Present","reason","r")), target("Present"))) {
            if (b.containsKey("sessionId")) b.put("attendanceId",1);
            correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ATTENDANCE_TARGET"));
        }
        var b=target("Present"); b.remove("memberId"); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ATTENDANCE_TARGET"));
        b=target("Present"); b.put("memberId",otherMember.getId()); correct(a,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_YOUR_CLASS"));
        jdbc.update("UPDATE invoices SET payment_status='Pending' WHERE class_id=?",course.getId());
        correct(a,target("Present")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_YOUR_CLASS")); assertEquals(0,count("attendances"));
    }
    @Test void completedNormalCanCreateAndFinalizerCannotOverwriteCorrection() throws Exception {
        var a=login(coach); jdbc.update("UPDATE class_enrollments SET status='Completed' WHERE class_id=?",course.getId());
        correct(a,target("Present")).andExpect(status().isOk()); finalizer.process(session.getId());
        assertEquals("Present",stored("state")); assertEquals(1,count("attendances")); assertEquals("Completed",jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,session.getId()));
    }
    @Test void finalizerCompletesNormalAtEndAndCorrectionWorksAfterLifecycle() throws Exception {
        tx(() -> { for(int n=1;n<12;n++) { var s=new Session(); s.setSportsClass(course); s.setCoach(coach); s.setRoom(room); s.setSessionDate(LocalDate.of(2026,10,20).plusDays(n)); s.setStartTime(LocalTime.of(6,0)); s.setEndTime(LocalTime.of(7,30)); s.setAttendanceFinalized(true); s.setStatus("Completed"); em.persist(s); } });
        finalizer.process(session.getId());
        assertEquals("Completed",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=?",String.class,course.getId())); assertEquals(0,notifications());
        correct(login(coach),target("Present")).andExpect(status().isOk()); clock.at("2026-11-10T00:00:00"); finalizer.review(session.getId()); finalizer.review(session.getId()); assertEquals(0,notifications());
    }
    @ParameterizedTest @ValueSource(strings={"Active","Completed","Expired"})
    void advancedHistoricalEligibilityPreservesCreditAndLifecycle(String entitlementStatus) throws Exception {
        tx(() -> { course.setClassType("Advanced"); course.setMaxCapacity(1); em.merge(course); var sub=new MemberPackage(); sub.setMember(member); sub.setSubject(subject); sub.setPrimaryCoach(coach); sub.setPurchaseAmount(new BigDecimal("900000")); sub.setActivatedAt(LocalDateTime.of(2026,11,1,8,0)); sub.setExpiresAt(LocalDateTime.of(2026,11,9,7,30)); sub.setStartDate(LocalDate.of(2026,11,1)); sub.setEndDate(LocalDate.of(2026,11,9)); sub.setStatus(entitlementStatus); sub.setRemainingCredit(0); em.persist(sub); var s=em.find(Session.class,session.getId()); s.setBookedMember(member); s.setMemberPackage(sub); paid(sub); });
        if ("Active".equals(entitlementStatus)) {
            finalizer.process(session.getId());
            assertEquals("Completed",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=(SELECT subscription_id FROM sessions WHERE session_id=?)",String.class,session.getId()));
        }
        correct(login(coach),target("Present")).andExpect(status().isOk());
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=(SELECT subscription_id FROM sessions WHERE session_id=?)",Integer.class,session.getId()));
    }
    @Test void correctionAndFinalizerRaceKeepOneCurrentRecord() throws Exception {
        var a=login(coach); var body=target("Present"); race(() -> finalizer.process(session.getId()), () -> { correct(a,body).andExpect(status().isOk()); });
        assertEquals(1,count("attendances")); assertEquals("Present",stored("state")); assertNull(stored("check_in_time")); assertEquals(1,count("system_logs")); assertEquals(0,notifications());
    }
    @Test void noShowWaitsForCutoffUsesCurrentAbsenceAndDoesNotRepeat() throws Exception {
        finalizer.process(session.getId()); finalizer.review(session.getId()); assertEquals(0,notifications());
        clock.at("2026-11-09T23:59:59.999"); finalizer.review(session.getId()); assertEquals(0,notifications());
        clock.at("2026-11-10T00:00:00"); race(() -> finalizer.review(session.getId()), () -> finalizer.review(session.getId())); finalizer.review(session.getId());
        assertEquals(1,notifications()); assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification_recipients r JOIN notifications n ON r.notification_id=n.notification_id WHERE n.dedupe_key=? AND r.user_id=?",Integer.class,"NO_SHOW_RECORDED:"+session.getId(),member.getId()));
        assertNull(jdbc.queryForObject("SELECT booking_blocked_until FROM members WHERE user_id=?",String.class,member.getId()));
    }
    @Test void legacyFinalizedSessionIsNotQueuedForHistoricalNotifications() {
        jdbc.update("UPDATE sessions SET attendance_finalized=true,status='Completed' WHERE session_id=?",session.getId()); clock.at("2026-11-10T00:00:00"); finalizer.process(session.getId()); finalizer.review(session.getId()); assertEquals(0,notifications());
    }
    @Test void enabledSanctionCommitsSevenDaysOnlyAfterCutoffAndDoesNotExtendOnRetry() {
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(finalizer,"bookingBlockEnabled",true);
            tx(() -> { for(int n=1;n<=2;n++) { var s=new Session(); s.setSportsClass(course); s.setCoach(coach); s.setRoom(room); s.setSessionDate(LocalDate.of(2026,11,9).minusDays(n)); s.setStartTime(LocalTime.of(6,0)); s.setEndTime(LocalTime.of(7,30)); s.setStatus("Completed"); s.setAttendanceFinalized(true); em.persist(s); var a=new Attendance(); a.setSession(s); a.setMember(member); a.setState("Absent"); em.persist(a); } });
            finalizer.process(session.getId()); finalizer.review(session.getId());
            assertNull(jdbc.queryForObject("SELECT booking_blocked_until FROM members WHERE user_id=?",String.class,member.getId()));
            clock.at("2026-11-10T00:00:00"); finalizer.review(session.getId());
            assertEquals(LocalDateTime.of(2026,11,17,0,0),jdbc.queryForObject("SELECT booking_blocked_until FROM members WHERE user_id=?",LocalDateTime.class,member.getId()));
            int messages=jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id=? AND title='Class booking restricted'",Integer.class,member.getId());
            assertEquals(1,messages); clock.at("2026-11-11T00:00:00"); finalizer.review(session.getId());
            assertEquals(messages,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id=? AND title='Class booking restricted'",Integer.class,member.getId()));
            assertEquals(LocalDateTime.of(2026,11,17,0,0),jdbc.queryForObject("SELECT booking_blocked_until FROM members WHERE user_id=?",LocalDateTime.class,member.getId()));
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(finalizer,"bookingBlockEnabled",false);
            jdbc.update("UPDATE members SET booking_blocked_until=NULL WHERE user_id=?",member.getId());
        }
    }
    @Test void inactiveMissingPermissionAndWrongRoleCannotCorrect() throws Exception {
        var a=login(coach); jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",coach.getId());
        try { correct(a,target("Present")).andExpect(status().isForbidden()); } finally { jdbc.update("UPDATE users SET status='Active' WHERE user_id=?",coach.getId()); }
        a=login(coach); int roleId=coach.getRole().getId();
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?",roleId);
        try { correct(a,target("Present")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ATTENDANCE_CORRECTION_FORBIDDEN")); }
        finally { jdbc.update("INSERT INTO role_permissions(role_id,permission_id) SELECT ?,permission_id FROM permissions WHERE permission_name='RECORD_RESULT'",roleId); }
        correct(login(receptionist),target("Present")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ATTENDANCE_CORRECTION_FORBIDDEN")); assertEquals(0,count("attendances"));
    }
    @Test void cancelledInvalidatedAndSupersededRemainProtected() throws Exception {
        var a=login(coach); finalizer.process(session.getId()); int id=jdbc.queryForObject("SELECT attendance_id FROM attendances WHERE session_id=?",Integer.class,session.getId());
        jdbc.update("UPDATE attendances SET superseded_at=? WHERE attendance_id=?",LocalDateTime.of(2026,11,9,7,30),id);
        correct(a,new LinkedHashMap<>(Map.of("attendanceId",id,"newState","Present","reason","r"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ATTENDANCE_SUPERSEDED"));
        jdbc.update("UPDATE sessions SET status='Cancelled' WHERE session_id=?",session.getId()); correct(a,target("Present")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SESSION_NOT_HELD"));
        jdbc.update("UPDATE sessions SET status='Completed',center_invalidated=true WHERE session_id=?",session.getId()); correct(a,target("Present")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SESSION_NOT_HELD"));
    }
    private interface Checked { void run() throws Exception; }
    @Test void bulkNoteReadbackAndInvalidItemRollback() throws Exception {
        var a=login(coach); clock.at("2026-11-09T07:00:00");
        Map<String,Object> body=Map.of("sessionId",session.getId(),"coachId",coach.getId(),"items",List.of(Map.of("memberId",member.getId(),"attendanceStatus","Present","content","Bulk assessment","note","Bulk public note")));
        mvc.perform(post("/api/training-results/bulk").session(a).contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].note").value("Bulk public note"));
        body=Map.of("sessionId",session.getId(),"items",List.of(Map.of("memberId",member.getId(),"attendanceStatus","Absent","note","Should rollback"),Map.of("memberId",member.getId(),"attendanceStatus","Present","note","x".repeat(1001))));
        mvc.perform(post("/api/training-results/bulk").session(a).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOTE_TOO_LONG"));
        assertEquals("Present",stored("state")); assertEquals("Bulk public note",stored("note"));
        mvc.perform(get("/api/training-results").param("sessionId",session.getId().toString()).session(login(member))).andExpect(status().isOk()).andExpect(jsonPath("$[0].note").value("Bulk public note"));
    }
    @Test void originalAbsenceReasonAndLinkSurviveCorrection() throws Exception {
        final int[] noticeId={0}; tx(() -> { var n=new SessionAbsenceNotice(); n.setSession(session); n.setMemberId(member.getId()); n.setOriginalStart(LocalDateTime.of(2026,11,9,6,0)); n.setOriginalEnd(LocalDateTime.of(2026,11,9,7,30)); n.setReason("Original absence reason"); n.setReportedAt(LocalDateTime.of(2026,11,9,5,0)); em.persist(n); noticeId[0]=n.getId();
            var a=new Attendance(); a.setSession(session); a.setMember(member); a.setState("Absent"); a.setAbsenceNoticeId(n.getId()); em.persist(a); });
        var body=target("Present"); body.put("note","Public reconciliation"); correct(login(coach),body).andExpect(status().isOk()).andExpect(jsonPath("$.absenceNoticeId").value(noticeId[0]));
        assertEquals("Original absence reason",jdbc.queryForObject("SELECT reason FROM session_absence_notices WHERE id=?",String.class,noticeId[0]));
        assertEquals("Public reconciliation",stored("note")); assertNull(stored("check_in_time"));
    }
    @Test void openApiMatchesImplementedAttendanceFields() throws Exception {
        var spec=json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        var schemas=spec.path("components").path("schemas");
        for(String field:List.of("note","correctionAllowed","correctionDeadline","serverTime")) assertTrue(schemas.path("AttendanceResponse").path("properties").has(field),field);
        for(String field:List.of("attendanceId","sessionId","memberId","newState","reason","note")) assertTrue(schemas.path("AttendanceCorrectionRequest").path("properties").has(field),field);
        assertTrue(schemas.path("TrainingResultResponse").path("properties").has("note"));
        String out=System.getProperty("scms.attendance.openapi", "");
        if(!out.isBlank()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode)spec).putArray("servers").addObject().put("url","https://scms-app-p1gy.onrender.com").put("description","Canonical FE server; schema generated locally");
            json.writerWithDefaultPrettyPrinter().writeValue(java.nio.file.Path.of(out).toFile(),spec);
        }
    }
    @Test void metadataUsesVietnamClockIndependentOfClockZone() {
        var metadata=SessionAttendancePolicy.metadata(session,member,Clock.fixed(Instant.parse("2026-11-09T17:00:00Z"),ZoneOffset.UTC));
        assertEquals("2026-11-10T00:00+07:00",metadata.serverTime().toString()); assertFalse(metadata.correctionAllowed());
    }
    private void race(Checked first, Checked second) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try { var futures=new ArrayList<Future<?>>(); for(var action:List.of(first,second)) futures.add(pool.submit(() -> { ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new AssertionError("Race start"); action.run(); return null; }));
            assertTrue(ready.await(10,TimeUnit.SECONDS)); start.countDown(); for(var f:futures) f.get(30,TimeUnit.SECONDS);
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
