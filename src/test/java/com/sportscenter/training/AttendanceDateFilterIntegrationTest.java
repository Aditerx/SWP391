package com.sportscenter.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** D15/R1 Owner12/11: session date, legacy lobby check-in date, unchanged read scopes and data. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:attendance_dates;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "scms.lobby-checkin.enabled=false", "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false",
        "vnpay.network-probe-enabled=false", "scms.ai.chat.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceDateFilterIntegrationTest {
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.attendance-date.pg-url", ""); if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+")) throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url",()->url); r.add("spring.datasource.username",()->"postgres"); r.add("spring.datasource.password",()->"scms-local-only");
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver"); r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect"); r.add("spring.jpa.hibernate.ddl-auto",()->"none");
    }
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @MockBean com.sportscenter.invoice.PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;

    private User memberA, memberB, memberC, memberD, coachA, coachB, coachD, manager, manager2, reception;
    private Session today, previous, next, otherCoachSession, crossCenterSession;
    private int present, absent, presentNull, previousAbsent, nextPresent, legacyDifferentDay;
    private int lobbyStart, lobbyLast, lobbyPrevious, lobbyNext, lobbyUnknown, otherCoach, crossCenter, superseded;
    private static final LocalDate DAY=LocalDate.of(2026,11,12);
    private static final String PASSWORD="Attendance-dates-local-2026!";
    private static final String DAY_QUERY="?startDate=2026-11-12&endDate=2026-11-12";

    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t->work.run()); }
    @BeforeAll void committedSyntheticFixture() {
        tx(()->{
            if (System.getProperty("scms.attendance-date.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Attendance date1'),(2,'Attendance date2') ON CONFLICT(center_id) DO NOTHING");
            Permission record=permission("RECORD_RESULT"), manage=permission("MANAGE_CLASSES"), register=permission("REGISTER_MEMBER");
            Role mr=role("Member"), cr=role("Coach",record), managers=role("CenterManager",manage), rr=role("Receptionist",register);
            memberA=user("member-a",mr,1); memberB=user("member-b",mr,1); memberC=user("member-c",mr,1); memberD=user("member-d",mr,2);
            coachA=user("coach-a",cr,1); coachB=user("coach-b",cr,1); coachD=user("coach-d",cr,2);
            manager=user("manager",managers,1); manager2=user("manager2",managers,2); reception=user("reception",rr,1);
            Subject subject=em.createQuery("select s from Subject s where s.slug='cau-long'",Subject.class).getResultStream().findFirst().orElse(null);
            if (subject==null) { subject=new Subject(); subject.setName("Date fixture badminton"); subject.setSlug("cau-long"); subject.setNormalPrice(new BigDecimal("900000")); subject.setAdvancedPrice(new BigDecimal("3000000")); em.persist(subject); }
            var class1=course("Date fixture1",subject,coachA,1); var class2=course("Date fixture2",subject,coachD,2);
            var room1=room("Date fixture court1",1); var room2=room("Date fixture court2",2);
            today=session(class1,room1,coachA,DAY); previous=session(class1,room1,coachA,DAY.minusDays(1)); next=session(class1,room1,coachA,DAY.plusDays(1));
            otherCoachSession=session(class1,room1,coachB,DAY); crossCenterSession=session(class2,room2,coachD,DAY);
            present=attendance(today,memberA,coachA,"Present",DAY.atTime(6,5),"Present note",false);
            absent=attendance(today,memberB,coachA,"Absent",null,"Absent note",false);
            presentNull=attendance(today,memberC,coachA,"Present",null,"Verified without check-in timestamp",false);
            previousAbsent=attendance(previous,memberA,coachA,"Absent",null,"Previous day",false);
            nextPresent=attendance(next,memberA,coachA,"Present",DAY.plusDays(1).atTime(6,5),"Next day",false);
            // Compatibility fixture only: imported legacy timestamp differs from its session day.
            legacyDifferentDay=attendance(previous,memberB,coachA,"Present",DAY.atStartOfDay(),"Synthetic legacy differing check-in date",false);
            lobbyStart=attendance(null,memberA,reception,"CheckedIn",DAY.atStartOfDay(),"Lobby midnight",false);
            lobbyLast=attendance(null,memberA,reception,"CheckedIn",DAY.atTime(23,59,59,999999000),"Lobby last PG microsecond",false);
            lobbyPrevious=attendance(null,memberA,reception,"CheckedIn",DAY.minusDays(1).atTime(23,59,59,999999000),"Lobby previous",false);
            lobbyNext=attendance(null,memberA,reception,"CheckedIn",DAY.plusDays(1).atStartOfDay(),"Lobby next midnight",false);
            lobbyUnknown=attendance(null,memberA,reception,"CheckedIn",null,"Legacy unknown lobby timestamp",false);
            otherCoach=attendance(otherCoachSession,memberB,coachB,"Absent",null,"Other coach same center",false);
            crossCenter=attendance(crossCenterSession,memberD,coachD,"Absent",null,"Other center",false);
            superseded=attendance(today,memberB,coachA,"Absent",DAY.atTime(6,0),"Superseded history",true);
        });
    }
    private Permission permission(String name) { var p=new Permission(); p.setName(name); em.persist(p); return p; }
    private Role role(String name,Permission... ps) { var r=new Role(); r.setName(name); r.getPermissions().addAll(List.of(ps)); em.persist(r); return r; }
    private User user(String name,Role role,int center) {
        var u=new User(); u.setEmail(name+"@attendance-date.test.invalid"); u.setFullName(name); u.setRole(role); u.setCenterId(center); u.setStatus("Active"); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u);
        if ("Member".equals(role.getName())) { var m=new Member(); m.setUser(u); m.setJoinDate(DAY.minusDays(10)); em.persist(m); }
        if ("Coach".equals(role.getName())) { var c=new Coach(); c.setUser(u); em.persist(c); }
        if ("Receptionist".equals(role.getName())) { var r=new Receptionist(); r.setUser(u); em.persist(r); }
        return u;
    }
    private SportsClass course(String name,Subject subject,User coach,int center) {
        var c=new SportsClass(); c.setName(name); c.setSubject(subject); c.setCoach(coach); c.setCenterId(center); c.setMaxCapacity(15); c.setStatus("Closed"); c.setTuitionFee(new BigDecimal("900000")); em.persist(c); return c;
    }
    private Room room(String name,int center) { var r=new Room(); r.setName(name); r.setCenterId(center); r.setCapacity(15); r.setStatus("Available"); em.persist(r); return r; }
    private Session session(SportsClass course,Room room,User coach,LocalDate date) {
        var s=new Session(); s.setSportsClass(course); s.setRoom(room); s.setCoach(coach); s.setSessionDate(date); s.setStartTime(LocalTime.of(6,0)); s.setEndTime(LocalTime.of(7,30)); em.persist(s); return s;
    }
    private int attendance(Session session,User member,User recorder,String state,LocalDateTime checkIn,String note,boolean old) {
        var a=new Attendance(); a.setSession(session); a.setMember(member); a.setRecordedBy(recorder); a.setState(state); a.setCheckInTime(checkIn); a.setNote(note); if (old) a.setSupersededAt(DAY.atTime(8,0)); em.persist(a); return a.getAttendanceId();
    }
    private HttpResponse<String> send(HttpClient client,String path,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30));
        if (body==null) b.GET(); else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient login(User actor) throws Exception {
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(200,send(client,"/api/auth/login",Map.of("email",actor.getEmail(),"password",PASSWORD)).statusCode());
        var me=send(client,"/api/auth/me",null); assertEquals(200,me.statusCode()); var actual=json.readTree(me.body());
        assertEquals(actor.getId(),actual.path("userId").asInt()); assertEquals(actor.getRole().getName(),actual.path("role").asText()); return client;
    }
    private JsonNode results(HttpClient client,String route,String query,int... expected) throws Exception {
        var r=send(client,route+query,null); assertEquals(200,r.statusCode()); var body=json.readTree(r.body());
        var ids=new ArrayList<Integer>(); body.forEach(x->ids.add(x.path("attendanceId").asInt()));
        assertEquals(Arrays.stream(expected).boxed().sorted(Comparator.reverseOrder()).toList(),ids,query); return body;
    }
    private JsonNode results(HttpClient client,String query,int... expected) throws Exception { return results(client,"/api/attendances",query,expected); }
    private List<Object> snapshot() {
        return List.of(jdbc.queryForList("SELECT * FROM attendances ORDER BY attendance_id"),jdbc.queryForList("SELECT * FROM training_results ORDER BY result_id"),
                jdbc.queryForList("SELECT * FROM sessions ORDER BY session_id"),jdbc.queryForList("SELECT * FROM member_packages ORDER BY subscription_id"),
                jdbc.queryForList("SELECT * FROM invoices ORDER BY invoice_id"),jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"));
    }

    @Test void sessionDayIncludesPresentAndAbsentWithNullCheckInAndPreservesResponse() throws Exception {
        var client=login(manager); var before=snapshot(); var rows=results(client,DAY_QUERY+"&sessionId="+today.getId(),present,absent,presentNull);
        for (JsonNode row:rows) {
            assertEquals(DAY.toString(),row.path("sessionDate").asText());
            if (row.path("attendanceId").asInt()==absent) { assertEquals("Absent",row.path("state").asText()); assertTrue(row.path("checkInTime").isNull()); assertEquals("Absent note",row.path("note").asText()); }
        }
        assertEquals(before,snapshot());
    }
    @Test void sessionDateWinsOverDifferentLegacyCheckInDateWithoutRewritingIt() throws Exception {
        var client=login(manager); var before=snapshot();
        var rows=results(client,"?startDate=2026-11-11&endDate=2026-11-11&sessionId="+previous.getId(),previousAbsent,legacyDifferentDay);
        var legacy=rows.get(0); assertEquals(legacyDifferentDay,legacy.path("attendanceId").asInt());
        assertEquals("2026-11-11",legacy.path("sessionDate").asText()); assertTrue(legacy.path("checkInTime").asText().startsWith("2026-11-12T00:00"));
        results(client,DAY_QUERY+"&sessionId="+previous.getId()); assertEquals(before,snapshot());
    }
    @Test void openBoundsAndBothEndDatesAreInclusive() throws Exception {
        var client=login(manager); var before=snapshot();
        results(client,"?startDate=2026-11-12",present,absent,presentNull,nextPresent,lobbyStart,lobbyLast,lobbyNext,otherCoach);
        results(client,"?endDate=2026-11-12",present,absent,presentNull,previousAbsent,legacyDifferentDay,lobbyStart,lobbyLast,lobbyPrevious,otherCoach);
        results(client,"?startDate=2026-11-11&endDate=2026-11-13",present,absent,presentNull,previousAbsent,nextPresent,legacyDifferentDay,lobbyStart,lobbyLast,lobbyPrevious,lobbyNext,otherCoach);
        assertEquals(before,snapshot());
    }
    @Test void lobbyUsesOriginalTimestampDayWithMidnightBoundariesAndUnknownExcluded() throws Exception {
        var client=login(manager); var before=snapshot();
        results(client,DAY_QUERY+"&state=CheckedIn",lobbyStart,lobbyLast);
        results(client,"?startDate=2026-11-11&endDate=2026-11-11&state=CheckedIn",lobbyPrevious);
        results(client,"?startDate=2026-11-13&endDate=2026-11-13&state=CheckedIn",lobbyNext);
        results(client,"?state=CheckedIn",lobbyStart,lobbyLast,lobbyPrevious,lobbyNext,lobbyUnknown); assertEquals(before,snapshot());
    }
    @Test void noDateFilterKeepsUnknownLobbyAndExcludesSupersededOrOtherCenter() throws Exception {
        var client=login(manager); var before=snapshot();
        results(client,"",present,absent,presentNull,previousAbsent,nextPresent,legacyDifferentDay,lobbyStart,lobbyLast,lobbyPrevious,lobbyNext,lobbyUnknown,otherCoach);
        assertEquals(14,jdbc.queryForObject("SELECT count(*) FROM attendances",Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT superseded_at FROM attendances WHERE attendance_id=?",java.sql.Timestamp.class,superseded)); assertEquals(before,snapshot());
    }
    @Test void filtersRemainAndCombinedWithCaseInsensitiveStateAndRecorder() throws Exception {
        var client=login(manager); var before=snapshot();
        results(client,DAY_QUERY+"&sessionId="+today.getId()+"&memberId="+memberB.getId()+"&recordedBy="+coachA.getId()+"&state=aBsEnT",absent);
        results(client,DAY_QUERY+"&recordedBy="+reception.getId(),lobbyStart,lobbyLast);
        results(client,DAY_QUERY+"&sessionId="+today.getId()+"&state=Absent&memberId="+memberA.getId()); assertEquals(before,snapshot());
    }
    @Test void memberOwnerScopeIsRetainedWithDatesAndCannotSelectOtherMember() throws Exception {
        var client=login(memberA); var before=snapshot(); results(client,DAY_QUERY,present,lobbyStart,lobbyLast);
        assertEquals(403,send(client,"/api/attendances"+DAY_QUERY+"&memberId="+memberB.getId(),null).statusCode()); assertEquals(before,snapshot());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void coachAssignmentSameCenterIsRetainedAndExcludesLobby(boolean useOtherCoach) throws Exception {
        var client=login(useOtherCoach?coachB:coachA); var before=snapshot();
        if (useOtherCoach) results(client,DAY_QUERY,otherCoach); else results(client,DAY_QUERY,present,absent,presentNull);
        results(client,DAY_QUERY+"&state=CheckedIn"); assertEquals(before,snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"manager","manager2","reception"})
    void existingCenterScopesAreRetainedForAuthorizedStaff(String role) throws Exception {
        var actor="manager2".equals(role)?manager2:"reception".equals(role)?reception:manager;
        var client=login(actor); var before=snapshot();
        if (actor==manager2) results(client,DAY_QUERY,crossCenter); else results(client,DAY_QUERY,present,absent,presentNull,lobbyStart,lobbyLast,otherCoach);
        assertEquals(before,snapshot());
    }
    @Test void canonicalAndSingularAliasReturnSameScopedRows() throws Exception {
        var client=login(manager); var before=snapshot();
        for (String route:List.of("/api/attendances","/api/attendance")) results(client,route,DAY_QUERY,present,absent,presentNull,lobbyStart,lobbyLast,otherCoach);
        assertEquals(before,snapshot());
    }
    @Test void inactiveAndRevokedAuthorityStillBlockExistingSession() throws Exception {
        var client=login(manager); int role=manager.getRole().getId(); int permission=jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'",Integer.class);
        try {
            jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",manager.getId()); var before=snapshot();
            assertEquals(403,send(client,"/api/attendances"+DAY_QUERY,null).statusCode()); assertEquals(before,snapshot());
            jdbc.update("UPDATE users SET status='Active' WHERE user_id=?",manager.getId()); jdbc.update("DELETE FROM role_permissions WHERE role_id=?",role); before=snapshot();
            assertEquals(403,send(client,"/api/attendances"+DAY_QUERY,null).statusCode()); assertEquals(before,snapshot());
        } finally {
            jdbc.update("UPDATE users SET status='Active' WHERE user_id=?",manager.getId()); jdbc.update("DELETE FROM role_permissions WHERE role_id=?",role); jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES(?,?)",role,permission);
        }
    }
    @Test void anonymousAndInvalidDateKeepExistingHttpGates() throws Exception {
        assertEquals(401,send(HttpClient.newHttpClient(),"/api/attendances"+DAY_QUERY,null).statusCode());
        var client=login(manager); var before=snapshot(); assertEquals(400,send(client,"/api/attendances?startDate=not-a-date",null).statusCode()); assertEquals(before,snapshot());
    }

    @Test void openApiKeepsDateAndResponseShapeAndExplainsSessionAndLobbyMeaning() throws Exception {
        var response=send(HttpClient.newHttpClient(),"/v3/api-docs",null); assertEquals(200,response.statusCode()); var spec=json.readTree(response.body());
        for (String route:List.of("/api/attendances","/api/attendance")) {
            Set<String> names=new HashSet<>();
            for (JsonNode parameter:spec.path("paths").path(route).path("get").path("parameters")) {
                String name=parameter.path("name").asText(); names.add(name);
                if (Set.of("startDate","endDate").contains(name)) {
                    assertEquals("date",parameter.path("schema").path("format").asText());
                    String description=parameter.path("description").asText();
                    assertTrue(description.contains("sessionDate") && description.contains("checkInTime") && description.toLowerCase(Locale.ROOT).contains("inclusive"));
                }
            }
            assertEquals(Set.of("sessionId","memberId","recordedBy","state","startDate","endDate"),names);
        }
        var properties=spec.path("components").path("schemas").path("AttendanceResponse").path("properties");
        assertTrue(properties.has("sessionDate") && properties.has("checkInTime") && properties.has("note"));
        String output=System.getProperty("scms.attendance-date.openapi", "");
        if (!output.isBlank()) {
            ((ObjectNode)spec).putArray("servers").addObject().put("url","http://localhost").put("description","Generated server url");
            Path path=Path.of(output); Files.createDirectories(path.toAbsolutePath().getParent()); Files.writeString(path,json.writerWithDefaultPrettyPrinter().writeValueAsString(spec));
        }
    }
}
