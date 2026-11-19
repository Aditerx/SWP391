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

/** D15/RP-36, Session2.3: loopback HTTP, real login/auth-me, committed synthetic data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:training_plan_history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false",
        "scms.ai.chat.enabled=false"
})
@Import(TrainingPlanHistoryIntegrationTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingPlanHistoryIntegrationTest {
    static class TestClock extends Clock {
        Instant current;
        public ZoneId getZone() { return SessionAttendancePolicy.ZONE; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        public Instant instant() { return current; }
        void at(String time) { current = LocalDateTime.parse("2026-11-12T" + time).atZone(getZone()).toInstant(); }
    }
    @TestConfiguration static class Config {
        @Bean @Primary TestClock contentClock() { var clock = new TestClock(); clock.at("06:30:00"); return clock; }
    }
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url = System.getProperty("scms.plan-history.pg-url", "");
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
    private Permission record, manage, planPermission;
    private TrainingPlan shared, ownPrivate, otherPrivate, receivingPlan;
    private SportsClass receiving;
    private Evaluation ownEvaluation;
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
            if (System.getProperty("scms.plan-history.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Content local A'),(2,'Content local B') ON CONFLICT(center_id) DO NOTHING");
            record = permission("RECORD_RESULT"); manage = permission("MANAGE_CLASSES");
            planPermission = permission("MANAGE_TRAINING_PLAN");
            coachRole = role("Coach", record, planPermission);
            coach = user("coach-a", coachRole); coachB = user("coach-b", coachRole);
            var memberRole = role("Member", record, manage, planPermission, permission("MANAGE_USERS"));
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
        var u = new User(); u.setEmail(name + "@plan-history.test.invalid"); u.setFullName(name); u.setRole(role); u.setCenterId(1);
        u.setStatus("Active"); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u);
        if ("Coach".equals(role.getName())) { var c = new Coach(); c.setUser(u); em.persist(c); }
        if ("Member".equals(role.getName())) { var m = new Member(); m.setUser(u); m.setJoinDate(DATE.minusDays(10)); em.persist(m); }
        if ("Receptionist".equals(role.getName())) { var r = new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    @BeforeEach void fixture() {
        clock.at("08:00:00"); sequence++;
        tx(() -> {
            var r = em.find(Role.class, coachRole.getId()); r.getPermissions().clear(); r.getPermissions().add(record); r.getPermissions().add(planPermission);
            for (User u : List.of(coach, coachB, member, memberB, outsider)) { var current = em.find(User.class, u.getId()); current.setStatus("Active"); current.setCenterId(1); }
            course = new SportsClass(); course.setName("Content synthetic " + sequence); course.setClassType("Normal");
            course.setSubject(subject); course.setCoach(coach); course.setCenterId(1); course.setMaxCapacity(15);
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
            shared = plan(coach, course, null, "Shared main");
            ownPrivate = plan(coach, null, member, "Own private history");
            ownPrivate.setContent(null); ownPrivate.setGoal(null); ownPrivate.setStartDate(null); ownPrivate.setEndDate(null);
            otherPrivate = plan(coach, null, memberB, "Other private history");
            receiving = new SportsClass(); receiving.setName("Receiving synthetic " + sequence); receiving.setClassType("Normal");
            receiving.setSubject(subject); receiving.setCoach(coachB); receiving.setCenterId(1); receiving.setMaxCapacity(15);
            receiving.setStatus("Ongoing"); receiving.setTuitionFee(new BigDecimal("900000")); em.persist(receiving);
            receivingPlan = plan(coachB, receiving, null, "Receiving shared");
            ownEvaluation = new Evaluation(); ownEvaluation.setMember(member); ownEvaluation.setCoach(coach);
            ownEvaluation.setEvaluationDate(DATE); ownEvaluation.setComment("Own evaluation history"); em.persist(ownEvaluation);
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
    private TrainingPlan plan(User author, SportsClass target, User owner, String title) {
        var p = new TrainingPlan(); p.setCoach(author); p.setSportsClass(target); p.setMember(owner);
        p.setTitle(title); p.setContent("DEMO stored plan content"); p.setGoal("DEMO goal");
        p.setStartDate(DATE.minusDays(10)); p.setEndDate(DATE); p.setCreatedAt(DATE.minusDays(10).atTime(8,0));
        em.persist(p); return p;
    }
    private int invoiceId() {
        return jdbc.queryForObject("SELECT invoice_id FROM invoices WHERE member_id=? AND class_id=?",Integer.class,member.getId(),course.getId());
    }
    private void enrollment(String state) {
        jdbc.update("UPDATE class_enrollments SET status=? WHERE member_id=? AND class_id=?",state,member.getId(),course.getId());
    }
    private List<Object> snapshot() {
        var snapshots = new ArrayList<Object>();
        for (String table : List.of("training_plans","training_results","attendances","evaluations","classes",
                "sessions","class_enrollments","invoices","member_packages","payment_resolutions","normal_transfers","system_logs")) {
            snapshots.add(jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1"));
        }
        return snapshots;
    }
    private Set<Integer> ids(HttpResponse<String> response) throws Exception {
        var ids = new HashSet<Integer>(); for (var p : json.readTree(response.body())) ids.add(p.path("planId").asInt()); return ids;
    }
    private void scope(HttpClient actor, TrainingPlan target, boolean allowed) throws Exception {
        var before = snapshot();
        var list = send(actor,"GET","/api/training-plans?classId="+target.getSportsClass().getId(),null);
        var detail = send(actor,"GET","/api/training-plans/"+target.getPlanId(),null);
        Set<Integer> visible = ids(list);
        assertAll("List/detail and committed state",
                () -> assertEquals(200,list.statusCode(),list.body()),
                () -> assertEquals(allowed ? Set.of(target.getPlanId()) : Set.of(),visible,list.body()),
                () -> assertEquals(allowed ? 200 : 403,detail.statusCode(),detail.body()),
                () -> assertEquals(before,snapshot(),"GET must not mutate plans, entitlement, money, history or audit"));
        if (allowed) {
            var row = json.readTree(detail.body()); assertEquals(target.getPlanId(),row.path("planId").asInt());
            assertEquals("DEMO stored plan content",row.path("content").asText());
            assertTrue(row.path("memberId").isNull()); assertEquals(target.getSportsClass().getId(),row.path("classId").asInt());
            assertEquals(row,json.readTree(list.body()).get(0));
        }
    }
    private void refund(Integer invoice, String status) {
        tx(() -> {
            var r = new PaymentResolution(); r.setInvoice(em.find(Invoice.class,invoice)); r.setType("Refund"); r.setStatus(status);
            r.setAmount(new BigDecimal("900000")); r.setReason("DEMO legacy refund predicate fixture");
            r.setEvidence("DEMO evidence"); r.setCreatedBy(actors.get("CenterManager").getId()); r.setCreatedAt(DATE.atTime(7,30));
            if ("Completed".equals(status)) {
                r.setApprovedBy(actors.get("CenterManager").getId()); r.setApprovedAt(DATE.atTime(7,40));
                r.setCompletedBy(actors.get("CenterManager").getId()); r.setCompletedAt(DATE.atTime(8,0));
                r.setPerformedAt(DATE.atTime(7,50)); r.setOutcomeNote("DEMO completed refund, no bank call");
            }
            em.persist(r);
        });
    }
    @ParameterizedTest @ValueSource(strings={"Registered","Completed"})
    void validPaidRegisteredOrCompletedReadsSameSharedPlan(String status) throws Exception {
        enrollment(status); if ("Completed".equals(status)) { jdbc.update("UPDATE classes SET status='Closed' WHERE class_id=?",course.getId()); jdbc.update("UPDATE training_plans SET end_date=? WHERE plan_id=?",DATE.minusDays(1),shared.getPlanId()); }
        scope(login(member),shared,true);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM member_packages WHERE member_id=?",Integer.class,member.getId()),"Normal history does not require an Advanced/general package");
    }
    @ParameterizedTest @ValueSource(strings={"Pending","Waitlisted","Cancelled"})
    void enrollmentWithoutLearningRightIsNotGrantedByPaidAlone(String status) throws Exception {
        enrollment(status); scope(login(member),shared,false);
    }
    @Test void paidWithoutOwnEnrollmentIsDenied() throws Exception {
        jdbc.update("DELETE FROM class_enrollments WHERE class_id=? AND member_id=?",course.getId(),member.getId()); scope(login(member),shared,false);
    }
    @ParameterizedTest @ValueSource(strings={"Missing","Pending","Reconciliation","Reversed","OtherMemberOnly"})
    void invalidOwnInvoiceProofDeniesListAndDetail(String variant) throws Exception {
        switch(variant) {
            case "Missing" -> jdbc.update("DELETE FROM invoices WHERE class_id=?",course.getId());
            case "OtherMemberOnly" -> jdbc.update("DELETE FROM invoices WHERE invoice_id=?",invoiceId());
            case "Pending" -> jdbc.update("UPDATE invoices SET payment_status='Pending' WHERE invoice_id=?",invoiceId());
            case "Reconciliation" -> jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE invoice_id=?",invoiceId());
            default -> jdbc.update("UPDATE invoices SET cash_confirmation_reversed=true WHERE invoice_id=?",invoiceId());
        }
        scope(login(member),shared,false);
    }
    @ParameterizedTest @ValueSource(strings={"Registered","Completed"})
    void completedRefundInvalidatesPaidForBothReadRoutes(String state) throws Exception {
        enrollment(state); refund(invoiceId(),"Completed"); scope(login(member),shared,false);
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoiceId()));
        assertEquals(0,new BigDecimal("900000").compareTo(jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoiceId())));
    }
    @Test void otherMembersRefundDoesNotInvalidateOwnValidInvoice() throws Exception {
        int otherInvoice=jdbc.queryForObject("SELECT invoice_id FROM invoices WHERE member_id=? AND class_id=?",Integer.class,memberB.getId(),course.getId());
        refund(otherInvoice,"Completed"); scope(login(member),shared,true);
    }
    @ParameterizedTest @ValueSource(strings={"Registered","Completed"})
    void transferUsesEffectiveClassAndPreservesOriginalInvoice(String targetState) throws Exception {
        int invoice=invoiceId(); enrollment("Cancelled");
        tx(() -> {
            em.find(Invoice.class,invoice).setEntitlementClass(em.find(SportsClass.class,receiving.getId()));
            var e=new ClassEnrollment();e.setMember(member);e.setSportsClass(receiving);e.setStatus(targetState);e.setEnrolledAt(DATE.minusDays(1).atTime(8,0));em.persist(e);
        });
        var client=login(member);scope(client,shared,false);scope(client,receivingPlan,true);
        assertEquals(course.getId(),jdbc.queryForObject("SELECT class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));
        assertEquals(receiving.getId(),jdbc.queryForObject("SELECT entitlement_class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));
        assertEquals(0,new BigDecimal("900000").compareTo(jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoice)));
    }
    @Test void originalPaidDoesNotGrantOldClassEvenWhenOldEnrollmentIsStaleRegistered() throws Exception {
        jdbc.update("UPDATE invoices SET entitlement_class_id=? WHERE invoice_id=?",receiving.getId(),invoiceId());scope(login(member),shared,false);
    }
    @Test void sharedPlanDoesNotBecomeAdvancedPrivatePlanThroughEnrollment() throws Exception {
        jdbc.update("UPDATE classes SET class_type='Advanced',max_capacity=1 WHERE class_id=?",course.getId());scope(login(member),shared,false);
    }
    @Test void anotherMemberCannotReadPrivateOrUnentitledSharedEvenWithExtraAuthorities() throws Exception {
        var client=login(outsider);scope(client,shared,false);var before=snapshot();
        var detail=send(client,"GET","/api/training-plans/"+ownPrivate.getPlanId(),null);assertEquals(403,detail.statusCode());
        var list=send(client,"GET","/api/training-plans",null);assertEquals(200,list.statusCode());assertEquals(Set.of(),ids(list));assertEquals(before,snapshot());
    }
    @Test void memberParameterCannotOverrideOwnerDespiteManageAuthorities() throws Exception {
        var client=login(member);var before=snapshot();var response=send(client,"GET","/api/training-plans?memberId="+memberB.getId(),null);
        assertEquals(403,response.statusCode());assertEquals(before,snapshot());
        response=send(client,"GET","/api/training-plans",null);assertEquals(200,response.statusCode());assertEquals(Set.of(shared.getPlanId(),ownPrivate.getPlanId()),ids(response));
    }
    @Test void coachAndClassFiltersOnlyNarrowTheMembersList() throws Exception {
        enrollment("Completed");var client=login(member);var before=snapshot();
        var response=send(client,"GET","/api/training-plans?memberId="+member.getId()+"&classId="+course.getId()+"&coachId="+coach.getId(),null);
        assertEquals(200,response.statusCode());assertEquals(Set.of(shared.getPlanId()),ids(response));
        response=send(client,"GET","/api/training-plans?classId="+course.getId()+"&coachId="+coachB.getId(),null);assertEquals(200,response.statusCode());assertEquals(Set.of(),ids(response));
        response=send(client,"GET","/api/training-plans?classId="+receiving.getId(),null);assertEquals(200,response.statusCode());assertEquals(Set.of(),ids(response));
        assertEquals(200,send(client,"GET","/api/training-plans/"+shared.getPlanId(),null).statusCode());assertEquals(before,snapshot());
    }
    @Test void centerScopeAppliesToBothRoutesEvenWithOwnPaidAndEnrollment() throws Exception {
        jdbc.update("UPDATE users SET center_id=2 WHERE user_id=?",member.getId());scope(login(member),shared,false);
    }
    @Test void oldCoachRetainsAuthorReadButNeitherCoachCanRewriteOldPlanAfterReassignment() throws Exception {
        jdbc.update("UPDATE classes SET coach_id=? WHERE class_id=?",coachB.getId(),course.getId());
        var old=login(coach);var next=login(coachB);var before=snapshot();
        assertEquals(200,send(old,"GET","/api/training-plans/"+shared.getPlanId(),null).statusCode());
        assertEquals(403,send(next,"GET","/api/training-plans/"+shared.getPlanId(),null).statusCode());
        var response=send(old,"GET","/api/training-plans?classId="+course.getId(),null);assertEquals(200,response.statusCode());assertEquals(Set.of(shared.getPlanId()),ids(response));
        response=send(next,"GET","/api/training-plans?classId="+course.getId(),null);assertEquals(200,response.statusCode());assertEquals(Set.of(),ids(response));
        var body=Map.of("coachId",coach.getId(),"classId",course.getId(),"title","Must not overwrite");
        assertEquals(403,send(old,"PUT","/api/training-plans/"+shared.getPlanId(),body).statusCode());
        assertEquals(403,send(next,"PUT","/api/training-plans/"+shared.getPlanId(),body).statusCode());assertEquals(before,snapshot());
    }
    @Test void completedReadDoesNotGrantMemberProfessionalWrites() throws Exception {
        enrollment("Completed");var client=login(member);var before=snapshot();var body=Map.of("coachId",coach.getId(),"classId",course.getId(),"title","Not permitted");
        assertEquals(403,send(client,"POST","/api/training-plans",body).statusCode());assertEquals(403,send(client,"PUT","/api/training-plans/"+shared.getPlanId(),body).statusCode());
        assertEquals(before,snapshot());
    }
    @Test void cancellationDoesNotEraseExistingOwnPrivateResultAttendanceEvaluationHistory() throws Exception {
        enrollment("Cancelled");var client=login(member);var before=snapshot();
        var privateDetail=send(client,"GET","/api/training-plans/"+ownPrivate.getPlanId(),null);assertEquals(200,privateDetail.statusCode());
        var privateRow=json.readTree(privateDetail.body());
        for(String field:List.of("classId","className","content","goal","startDate","endDate"))assertTrue(privateRow.path(field).isNull(),field+" is an existing valid null payload");
        for(String path:List.of("/api/training-results?sessionId="+session.getId(),"/api/attendances?sessionId="+session.getId())) {
            var response=send(client,"GET",path,null);assertEquals(200,response.statusCode(),response.body());assertEquals(1,json.readTree(response.body()).size());
        }
        var evaluations=send(client,"GET","/api/evaluations?memberId="+member.getId(),null);assertEquals(200,evaluations.statusCode());
        boolean found=false;
        for(var evaluation:json.readTree(evaluations.body())) {
            assertEquals(member.getId(),evaluation.path("memberId").asInt());
            if(evaluation.path("evaluationId").asInt()==ownEvaluation.getEvaluationId())found=true;
        }
        assertTrue(found,"Current own evaluation must remain readable along with accumulated own history");
        assertEquals(200,send(client,"GET","/api/evaluations/"+ownEvaluation.getEvaluationId(),null).statusCode());assertEquals(before,snapshot());
    }
    @Test void entitlementChangeBetweenListAndDetailRequiresFreshAuthorization() throws Exception {
        var client=login(member);scope(client,shared,true);
        jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE invoice_id=?",invoiceId());scope(client,shared,false);
        jdbc.update("UPDATE invoices SET reconciliation_required=false WHERE invoice_id=?",invoiceId());enrollment("Completed");scope(client,shared,true);
    }
    @Test void nonexistentPlanRemains404AndAnonymousIsOnlyAuthenticationGate() throws Exception {
        var client=login(member);var before=snapshot();assertEquals(404,send(client,"GET","/api/training-plans/2147483647",null).statusCode());
        var anonymous=HttpClient.newHttpClient();assertEquals(401,send(anonymous,"GET","/api/training-plans",null).statusCode());
        assertEquals(401,send(anonymous,"GET","/api/training-plans/"+shared.getPlanId(),null).statusCode());assertEquals(before,snapshot());
    }
    @Test void openApiDescribesSharedHistoryWithoutChangingResponseOrQueryShape() throws Exception {
        var response=send(login(member),"GET","/v3/api-docs",null);assertEquals(200,response.statusCode());var spec=json.readTree(response.body());
        for(String path:List.of("/api/training-plans","/api/training-plans/{id}")) {
            String description=spec.path("paths").path(path).path("get").path("description").asText();
            for(String word:List.of("Registered","Completed","Paid","Cancelled","Refund"))assertTrue(description.contains(word),path+" missing "+word);
        }
        var requestParams=new HashSet<String>();for(var param:spec.path("paths").path("/api/training-plans").path("get").path("parameters"))requestParams.add(param.path("name").asText());
        assertEquals(Set.of("coachId","classId","memberId"),requestParams);
        assertEquals(13,spec.path("components").path("schemas").path("TrainingPlanResponse").path("properties").size());
        for(String field:List.of("classId","className","memberId","memberName","content","goal","startDate","endDate"))
            assertTrue(spec.path("components").path("schemas").path("TrainingPlanResponse").path("properties").path(field).path("nullable").asBoolean(),field+" must describe actual nullable payload");
        String out=System.getProperty("scms.plan-history.openapi","");
        if(!out.isBlank())json.writerWithDefaultPrettyPrinter().writeValue(java.nio.file.Path.of(out).toFile(),spec);
    }
}
