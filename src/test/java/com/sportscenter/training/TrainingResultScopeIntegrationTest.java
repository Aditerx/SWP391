package com.sportscenter.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.subject.Subject;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** D15 Session1.2: real loopback HTTP and committed synthetic center/owner/author fixtures. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:training_scope;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false", "scms.ai.chat.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingResultScopeIntegrationTest {
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.result-scope.pg-url", "");
        if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+"))
            throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url", () -> url); r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "scms-local-only");
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
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;

    private final Map<String,User> actors=new LinkedHashMap<>();
    private User memberA, memberB, memberC, coachA, coachB, coachC;
    private Session sessionA, sessionB, sessionC;
    private int own, sameSessionOtherMember, otherAuthor, otherCenter, historicOwn;
    private static final String PASSWORD="Scope-synthetic-local-2026!";
    private static final LocalDateTime RECORDED=LocalDateTime.of(2026,11,11,10,0);

    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t -> work.run()); }
    @BeforeAll void committedFixture() {
        tx(() -> {
            if (System.getProperty("scms.result-scope.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Scope local1'),(2,'Scope local2') ON CONFLICT(center_id) DO NOTHING");
            var classes=permission("MANAGE_CLASSES"); var users=permission("MANAGE_USERS");
            // Extra read authorities must not widen the Member/Coach branches.
            var memberRole=role("Member",classes,users); var coachRole=role("Coach",classes,users);
            memberA=user("member-a",memberRole,1); memberB=user("member-b",memberRole,1); memberC=user("member-c",memberRole,2);
            coachA=user("coach-a",coachRole,1); coachB=user("coach-b",coachRole,1); coachC=user("coach-c",coachRole,2);
            var managerRole=role("CenterManager",classes); var receptionRole=role("Receptionist",users); var adminRole=role("Admin",users);
            actors.put("manager1",user("manager1",managerRole,1)); actors.put("manager2",user("manager2",managerRole,2));
            actors.put("reception1",user("reception1",receptionRole,1)); actors.put("reception2",user("reception2",receptionRole,2));
            actors.put("adminGlobal",user("admin-global",adminRole,null)); actors.put("admin1",user("admin1",adminRole,1)); actors.put("admin2",user("admin2",adminRole,2));
            var subjects=em.createQuery("select s from Subject s where s.slug='cau-long'",Subject.class).getResultList();
            Subject subject;
            if (subjects.isEmpty()) {
                subject=new Subject(); subject.setName("Cầu lông"); subject.setSlug("cau-long");
                subject.setNormalPrice(new BigDecimal("900000")); subject.setAdvancedPrice(new BigDecimal("3000000")); em.persist(subject);
            } else subject=subjects.get(0);
            var class1=course("Scope1",subject,coachA,1); var class2=course("Scope2",subject,coachC,2);
            var room1=room("Scope court1",1); var room2=room("Scope court2",2);
            sessionA=session(class1,room1,coachA); sessionB=session(class1,room1,coachB); sessionC=session(class2,room2,coachC);
            own=result(sessionA,memberA,coachA); sameSessionOtherMember=result(sessionA,memberB,coachA);
            otherAuthor=result(sessionB,memberB,coachB); otherCenter=result(sessionC,memberC,coachC);
            // Own historical results remain readable after the account's current center/assignment changes.
            historicOwn=result(sessionC,memberA,coachA);
        });
    }
    private Permission permission(String name) { var p=new Permission(); p.setName(name); em.persist(p); return p; }
    private Role role(String name,Permission... ps) { var r=new Role(); r.setName(name); r.getPermissions().addAll(List.of(ps)); em.persist(r); return r; }
    private User user(String name,Role role,Integer center) {
        var u=new User(); u.setEmail(name+"@scope.test.invalid"); u.setFullName(name); u.setStatus("Active"); u.setCenterId(center); u.setRole(role); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u);
        if ("Member".equals(role.getName())) { var m=new Member(); m.setUser(u); m.setJoinDate(RECORDED.toLocalDate().minusDays(10)); em.persist(m); }
        if ("Coach".equals(role.getName())) { var c=new Coach(); c.setUser(u); em.persist(c); }
        if ("Receptionist".equals(role.getName())) { var r=new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    private SportsClass course(String name,Subject subject,User coach,int center) {
        var c=new SportsClass(); c.setName(name); c.setSubject(subject); c.setCoach(coach); c.setCenterId(center); c.setStatus("Ongoing"); c.setMaxCapacity(15); c.setTuitionFee(new BigDecimal("900000")); em.persist(c); return c;
    }
    private Room room(String name,int center) { var r=new Room(); r.setName(name); r.setCenterId(center); r.setCapacity(15); r.setStatus("Available"); em.persist(r); return r; }
    private Session session(SportsClass course,Room room,User coach) {
        var s=new Session(); s.setSportsClass(course); s.setRoom(room); s.setCoach(coach); s.setSessionDate(RECORDED.toLocalDate()); s.setStartTime(LocalTime.of(9,0)); s.setEndTime(LocalTime.of(10,0)); em.persist(s); return s;
    }
    private int result(Session session,User member,User coach) {
        var r=new TrainingResult(); r.setSession(session); r.setMember(member); r.setCoach(coach); r.setContent("Private "+member.getFullName()); r.setRecordedAt(RECORDED); em.persist(r);
        var a=new Attendance(); a.setSession(session); a.setMember(member); a.setRecordedBy(coach); a.setState("Present"); a.setNote("Private attendance "+member.getFullName()); a.setCheckInTime(RECORDED); em.persist(a); return r.getResultId();
    }
    private HttpResponse<String> send(HttpClient client,String path,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));
        if (body==null) b.GET(); else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient login(User actor) throws Exception {
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(200,send(client,"/api/auth/login",Map.of("email",actor.getEmail(),"password",PASSWORD)).statusCode());
        var me=send(client,"/api/auth/me",null); assertEquals(200,me.statusCode());
        assertEquals(actor.getId(),json.readTree(me.body()).path("userId").asInt());
        assertEquals(actor.getRole().getName(),json.readTree(me.body()).path("role").asText()); return client;
    }
    private void results(HttpClient client,String query,int... expected) throws Exception {
        var r=send(client,"/api/training-results"+query,null); assertEquals(200,r.statusCode());
        List<Integer> ids=new ArrayList<>(); for (JsonNode row:json.readTree(r.body())) ids.add(row.path("resultId").asInt());
        var sorted=Arrays.stream(expected).boxed().sorted(Comparator.reverseOrder()).toList(); assertEquals(sorted,ids,query);
    }
    private List<Object> snapshot() {
        return List.of(jdbc.queryForList("SELECT * FROM training_results ORDER BY result_id"),jdbc.queryForList("SELECT * FROM attendances ORDER BY attendance_id"),
                jdbc.queryForList("SELECT * FROM sessions ORDER BY session_id"),jdbc.queryForList("SELECT * FROM invoices ORDER BY invoice_id"),
                jdbc.queryForList("SELECT * FROM class_enrollments ORDER BY enrollment_id"),jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"));
    }
    @ParameterizedTest @ValueSource(strings={"manager1","reception1","admin1"})
    void center1StaffScopeUsesSessionClassAndCombinesFilters(String name) throws Exception {
        var client=login(actors.get(name)); var before=snapshot();
        results(client,"",own,sameSessionOtherMember,otherAuthor);
        results(client,"?sessionId="+sessionA.getId(),own,sameSessionOtherMember);
        results(client,"?memberId="+memberB.getId(),sameSessionOtherMember,otherAuthor);
        results(client,"?coachId="+coachA.getId(),own,sameSessionOtherMember);
        results(client,"?sessionId="+sessionA.getId()+"&memberId="+memberB.getId()+"&coachId="+coachA.getId(),sameSessionOtherMember);
        results(client,"?sessionId="+sessionB.getId()+"&memberId="+memberA.getId()+"&coachId="+coachB.getId());
        assertEquals(before,snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"manager2","reception2","admin2"})
    void center2ScopeFollowsSessionEvenWhenHistoricalAuthorMemberMovedCenter(String name) throws Exception {
        var client=login(actors.get(name)); var before=snapshot(); results(client,"",otherCenter,historicOwn);
        results(client,"?memberId="+memberA.getId()+"&coachId="+coachA.getId(),historicOwn); assertEquals(before,snapshot());
    }
    @Test void globalAdminWithReadAuthorityKeepsGlobalReadAndAllFilters() throws Exception {
        var client=login(actors.get("adminGlobal")); var before=snapshot();
        results(client,"",own,sameSessionOtherMember,otherAuthor,otherCenter,historicOwn);
        results(client,"?memberId="+memberA.getId(),own,historicOwn);
        results(client,"?sessionId="+sessionC.getId()+"&memberId="+memberA.getId()+"&coachId="+coachA.getId(),historicOwn);
        assertEquals(before,snapshot());
    }
    @Test void crossCenterFiltersAndClientScopeFieldsCannotExpandStaffRead() throws Exception {
        var client=login(actors.get("manager1")); var before=snapshot();
        results(client,"?sessionId="+sessionC.getId()); results(client,"?memberId="+memberC.getId()); results(client,"?coachId="+coachC.getId());
        results(client,"?sessionId="+sessionC.getId()+"&memberId="+memberC.getId()+"&coachId="+coachC.getId()+"&centerId=2&role=Admin&actor="+actors.get("adminGlobal").getId());
        results(client,"?centerId=2&role=Admin",own,sameSessionOtherMember,otherAuthor); assertEquals(before,snapshot());
    }
    @Test void memberOwnHistoryWinsOverExtraAuthoritiesAndCurrentCenter() throws Exception {
        var client=login(memberA); var before=snapshot(); results(client,"",own,historicOwn); results(client,"?sessionId="+sessionC.getId(),historicOwn);
        results(client,"?memberId="+memberA.getId()+"&coachId="+coachA.getId(),own,historicOwn);
        results(client,"?coachId="+coachB.getId());
        assertEquals(403,send(client,"/api/training-results?memberId="+memberB.getId(),null).statusCode()); assertEquals(before,snapshot());
    }
    @Test void coachAuthorHistoryWinsOverExtraAuthoritiesAndCurrentAssignment() throws Exception {
        var client=login(coachA); var before=snapshot(); results(client,"",own,sameSessionOtherMember,historicOwn);
        results(client,"?sessionId="+sessionC.getId(),historicOwn); results(client,"?memberId="+memberB.getId(),sameSessionOtherMember);
        assertEquals(403,send(client,"/api/training-results?coachId="+coachB.getId(),null).statusCode()); assertEquals(before,snapshot());
    }
    @Test void liveCenterChangeRefreshesScopeForExistingStaffSession() throws Exception {
        var actor=actors.get("manager1"); var client=login(actor);
        try {
            jdbc.update("UPDATE users SET center_id=2 WHERE user_id=?",actor.getId()); var before=snapshot();
            results(client,"",otherCenter,historicOwn); assertEquals(before,snapshot());
        } finally { jdbc.update("UPDATE users SET center_id=1 WHERE user_id=?",actor.getId()); }
    }
    @Test void globalAdminStillNeedsReadAuthorityAndActiveAccount() throws Exception {
        var admin=actors.get("adminGlobal"); var client=login(admin); int roleId=admin.getRole().getId();
        int permission=jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'",Integer.class);
        try {
            jdbc.update("DELETE FROM role_permissions WHERE role_id=?",roleId); var before=snapshot();
            assertEquals(403,send(client,"/api/training-results",null).statusCode()); assertEquals(before,snapshot());
            jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES(?,?)",roleId,permission);
            jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",admin.getId()); before=snapshot();
            assertEquals(403,send(client,"/api/training-results",null).statusCode()); assertEquals(before,snapshot());
        } finally {
            jdbc.update("UPDATE users SET status='Active' WHERE user_id=?",admin.getId());
            jdbc.update("DELETE FROM role_permissions WHERE role_id=?",roleId); jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES(?,?)",roleId,permission);
        }
    }
    @Test void anonymousIsOnlyAnAuthenticationGate() throws Exception {
        var before=snapshot(); assertEquals(401,send(HttpClient.newHttpClient(),"/api/training-results",null).statusCode()); assertEquals(before,snapshot());
    }
}
