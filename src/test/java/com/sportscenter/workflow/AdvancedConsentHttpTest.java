package com.sportscenter.workflow;

import com.sportscenter.user.*;
import com.sportscenter.user.Role;
import com.sportscenter.membership.*;
import com.sportscenter.subject.*;
import com.sportscenter.specialization.*;
import com.sportscenter.room.*;
import com.sportscenter.invoice.*;
import com.sportscenter.session.*;
import com.sportscenter.holiday.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP login and committed database assertions. Default H2; opt-in disposable PG only. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.url=jdbc:h2:mem:advanced_consent;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false", "spring.sql.init.mode=never",
        "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=phase-workflow-test-secret", "payment.expire-minutes=5",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "logging.level.root=WARN", "debug=false"})
@Import(AdvancedConsentHttpTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdvancedConsentHttpTest {
    public static class MutableClock extends Clock {
        volatile Instant current=Instant.parse("2026-10-27T01:00:00Z");
        public ZoneId getZone(){return ZoneId.of("Asia/Ho_Chi_Minh");}
        public Clock withZone(ZoneId zone){return Clock.fixed(current,zone);}
        public Instant instant(){return current;}
        public void advance(Duration d){current=current.plus(d);}
    }
    @TestConfiguration static class Config { @Bean @Primary MutableClock testClock(){return new MutableClock();} }
    @DynamicPropertySource static void pg(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.phases.pg-url","");
        if (!url.isBlank()) {
            if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+")) throw new IllegalArgumentException("Only the disposable phases database is allowed");
            r.add("spring.datasource.url",()->url); r.add("spring.datasource.username",()->"postgres"); r.add("spring.datasource.password",()->"scms-local-only");
            r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver"); r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");
            r.add("spring.jpa.hibernate.ddl-auto",()->Boolean.getBoolean("scms.phases.validate-schema")?"validate":"none");
        }
    }
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob normalLifecycleJob;
    @Autowired com.sportscenter.sportclass.NormalLifecycleService normalLifecycle;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired MemberRepository members;
    @Autowired CoachRepository coaches;
    @Autowired ReceptionistRepository receptionists;
    @Autowired SubjectRepository subjects;
    @Autowired SpecializationRepository specs;
    @Autowired CoachSpecializationRepository assignments;
    @Autowired RoomRepository rooms;
    @Autowired com.sportscenter.sportclass.SportsClassRepository classes;
    @Autowired HolidayRepository holidays;
    @Autowired InvoiceRepository invoices;
    @Autowired MemberPackageRepository subscriptions;
    @Autowired MemberPackageService subscriptionService;
    @Autowired InvoiceService invoiceService;
    @Autowired PaymentProperties paymentProperties;
    @Autowired com.sportscenter.training.AttendanceFinalizationProcessor finalizer;
    @Autowired SessionRepository sessionRepository;
    @Autowired com.sportscenter.training.AttendanceRepository attendanceRepository;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired MutableClock clock;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired com.sportscenter.training.TrainingResultService resultService;
    @Autowired com.sportscenter.training.AttendanceService attendanceService;
    @Autowired SessionActionPolicy sessionActionPolicy;
    void early(boolean enabled) {
        org.springframework.test.util.ReflectionTestUtils.setField(resultService,"earlyAttendanceEnabled",enabled);
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService,"earlyAttendanceEnabled",enabled);
        org.springframework.test.util.ReflectionTestUtils.setField(sessionActionPolicy,"earlyAttendanceEnabled",enabled);
    }
    @AfterEach void restoreEarlySwitch() { early(false);org.springframework.test.util.ReflectionTestUtils.setField(paymentProperties,"expireMinutes",5); }
    @LocalServerPort int port;
    int memberId,coachId,otherId,subjectId,roomId,invoiceId,subscriptionId,lateInvoiceId;
    HttpClient member,coach,manager,cashier,other;
    String invoiceCode;
    final List<String> trace=new ArrayList<>();
    final List<Map<String,Object>> exchanges=new ArrayList<>();
    final List<Map<String,Object>> acceptance=new ArrayList<>();
    final Map<HttpClient,String> actors=new IdentityHashMap<>();
    int caseStart;
    final Map<String,Object> openApiProof=new LinkedHashMap<>();
    String currentScenario="fixture";
    boolean precedingWorkflowFailed;
    @BeforeAll void setup() throws Exception {
        tx.executeWithoutResult(t->{
            jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 10000");
            if(jdbc.queryForObject("SELECT count(*) FROM centers WHERE center_id=1",Integer.class)==0) jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            Role mr=role("Member"), cr=role("Coach","RECORD_RESULT"), mgr=role("CenterManager","MANAGE_CLASSES","VIEW_REPORTS","MANAGE_INVOICES","MANAGE_SUBSCRIPTIONS"), rr=role("Receptionist","MANAGE_INVOICES","REGISTER_MEMBER");
            User m=user("phase.member",mr),c=user("phase.coach",cr); memberId=m.getId(); coachId=c.getId();
            otherId=user("phase.other",mr).getId(); user("phase.manager",mgr);
            User desk=user("phase.cashier",rr); Receptionist deskProfile=new Receptionist();deskProfile.setUser(desk);receptionists.save(deskProfile);
            for(User u:List.of(m,users.findById(otherId).orElseThrow())) {Member profile=new Member();profile.setUser(u);profile.setJoinDate(LocalDate.now(clock));members.save(profile);}
            Coach profile=new Coach();profile.setUser(c);coaches.save(profile);
            Subject s=subjects.findBySlug("cau-long").orElseGet(Subject::new);s.setName("Cầu lông");s.setSlug("cau-long");s.setOutdoor(false);s.setStatus("Active");s.setNormalPrice(new BigDecimal("900000"));s.setAdvancedPrice(new BigDecimal("3000000"));subjects.save(s);subjectId=s.getId();
            Specialization spec=new Specialization();spec.setName("phase-badminton");spec.setSubject(s);spec.setStatus("Active");specs.save(spec);assignments.save(new CoachSpecialization(profile,spec));
            Room r=new Room();r.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON);r.setName("Phase test room");r.setCapacity(15);r.setStatus("Available");r.setCenterId(1);r.setFacilityType("Indoor");rooms.save(r);roomId=r.getId();
            if(holidays.findByHolidayDate(LocalDate.of(2026,10,30)).isEmpty()) holidays.save(new Holiday(LocalDate.of(2026,10,30),"Phase closure","CENTER_EVENT",true));
        });
        member=login("phase.member");coach=login("phase.coach");manager=login("phase.manager");cashier=login("phase.cashier");other=login("phase.other");
        Set<String> slots=new HashSet<>();for(DayOfWeek d:DayOfWeek.values())for(int s:List.of(1,2,5,6,7))slots.add(d.name()+":"+s);
        call(manager,"PUT","/api/coaches/"+coachId+"/work-schedule",Map.of("weeklySlots",slots,"daysOff",List.of(),"acceptingNew",true),200);
    }
    Role role(String name,String...auth) {
        Role r=roles.findAll().stream().filter(x->name.equals(x.getName())).findFirst().orElseGet(Role::new);r.setName(name);
        for(String a:auth){Permission p=permissions.findAll().stream().filter(x->a.equals(x.getName())).findFirst().orElseGet(Permission::new);p.setName(a);permissions.save(p);r.getPermissions().add(p);}return roles.save(r);
    }
    User user(String prefix,Role role){User u=new User();u.setRole(role);u.setFullName(prefix);u.setEmail(prefix+"@example.test");u.setStatus("Active");u.setCenterId(1);u.setPasswordHash(passwords.encode("LocalTest123!"));return users.save(u);}
    HttpClient login(String prefix) throws Exception {
        HttpClient c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
        actors.put(c,prefix);
        var signedIn=call(c,"POST","/api/auth/login",Map.of("email",prefix+"@example.test","password","LocalTest123!"),200);
        var me=call(c,"GET","/api/auth/me",null,200);
        assertEquals(prefix+"@example.test",me.path("email").asText());
        assertEquals(signedIn.path("userId"),me.path("userId"));assertEquals(signedIn.path("role"),me.path("role"));
        assertEquals(signedIn.path("authorities"),me.path("authorities"));
        return c;
    }
    JsonNode call(HttpClient client,String method,String path,Object body,int expected) throws Exception {
        return call(client,method,path,body,expected,Map.of());
    }
    JsonNode call(HttpClient client,String method,String path,Object body,int expected,Map<String,String> headers) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
        headers.forEach(b::header);
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
        if(!path.equals("/api/auth/login")) synchronized(trace){trace.add((method+" "+path.replaceAll("vnp_SecureHash=[^&]*","vnp_SecureHash=[redacted]")+" => "+response.statusCode()+" "+(path.contains("checkin-code")?"[QR payload redacted]":response.body().replaceAll("vnp_SecureHash=[^\"&]*","vnp_SecureHash=[redacted]"))).stripTrailing());}
        if(!path.equals("/api/auth/login")) {
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("scenario",currentScenario);item.put("actor",actor(client));
            item.put("businessNow",LocalDateTime.now(clock).toString());
            item.put("method",method);item.put("path",path.replaceAll("vnp_SecureHash=[^&]*","vnp_SecureHash=[redacted]"));
            item.put("requestBody",safeRequest(body));item.put("expectedStatus",expected);item.put("actualStatus",response.statusCode());
            item.put("previewHeaderPresent",headers.containsKey("X-Preview-Token"));
            Object responseBody=response.body();
            if(!response.body().isBlank()&&response.statusCode()!=302) {
                JsonNode parsed=json.readTree(response.body());
                if(path.contains("checkin-code")&&parsed.isObject())((com.fasterxml.jackson.databind.node.ObjectNode)parsed).put("payload","[redacted]");
                responseBody=parsed;
            }
            item.put("responseBody",responseBody);synchronized(exchanges){exchanges.add(item);}
        }
        if(expected>0)assertEquals(expected,response.statusCode(),method+" "+path+": "+response.body());
        return response.body().isBlank()||response.statusCode()==302?json.createObjectNode():json.readTree(response.body());
    }
    String actor(HttpClient client) {
        if(client==member)return "MEMBER_A";if(client==other)return "MEMBER_B";
        if(client==coach)return "PRIMARY_COACH";if(client==manager)return "CENTER_MANAGER";
        if(client==cashier)return "RECEPTIONIST";return actors.getOrDefault(client,"UNKNOWN");
    }
    void proof(String id,String decisions,String fixture,String expected,String sql,Object...params) {
        // HTTP has returned; JdbcTemplate reads on a separate connection outside the request transaction.
        Map<String,Object> item=new LinkedHashMap<>();item.put("id",id);item.put("decisions",List.of(decisions.split(",")));
        item.put("scenario",currentScenario);item.put("fixture",fixture);item.put("expected",expected);
        item.put("exchangeIndexes",java.util.stream.IntStream.range(caseStart,exchanges.size()).boxed().toList());
        item.put("businessNow",LocalDateTime.now(clock).toString());item.put("stateQuery",sql);
        item.put("stateQueryParams",Arrays.asList(params));
        item.put("stateAfterCommit",jdbc.queryForList(sql,params));
        acceptance.add(item);caseStart=exchanges.size();
    }
    String rightsSql(){return "SELECT p.subscription_id,p.status,p.remaining_credit,p.purchase_amount,p.primary_coach_id,p.activated_at,p.expires_at,p.expiry_revoked_credit,i.payment_status,i.amount,i.reconciliation_required,i.cash_confirmation_reversed FROM member_packages p LEFT JOIN invoices i ON i.subscription_id=p.subscription_id WHERE p.subscription_id=?";}
    String cancellationSql(){return "SELECT s.status,s.credit_returned,p.remaining_credit FROM sessions s JOIN member_packages p ON p.subscription_id=s.subscription_id WHERE s.session_id=?";}
    JsonNode safeRequest(Object body) {
        JsonNode result=json.valueToTree(body);
        if(result.isObject())for(String key:List.of("code","password","oldPassword","newPassword","otp","payload"))
            if(result.has(key))((com.fasterxml.jackson.databind.node.ObjectNode)result).put(key,"[redacted]");
        return result;
    }
    JsonNode requestOrder(String key,String method,HttpClient owner) throws Exception {
        return requestOrder(key,method,owner,subjectId,coachId,coach);
    }
    JsonNode requestOrder(String key,String method,HttpClient owner,int selectedSubject,int selectedCoach,HttpClient decisionActor) throws Exception {
        var r=call(owner,"POST","/api/advanced-registrations",Map.of("subjectId",selectedSubject,"primaryCoachId",selectedCoach,"requestKey",key),200);
        int id=r.get("id").asInt();
        assertEquals(id,call(owner,"POST","/api/advanced-registrations",Map.of("subjectId",selectedSubject,"primaryCoachId",selectedCoach,"requestKey",key),200).get("id").asInt());
        call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),400);
        call(decisionActor,"POST","/api/advanced-registrations/"+id+"/decision",Map.of("accepted",true),200);
        var order=call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),200);
        assertEquals(order.get("invoiceId"),call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),200).get("invoiceId"));
        return order;
    }

    @BeforeEach void resetClock(TestInfo info) throws Exception {
        currentScenario=info.getDisplayName();caseStart=exchanges.size();
        var attempts=org.springframework.test.util.ReflectionTestUtils.getField(com.sportscenter.training.AttendanceService.class,"SCAN_ATTEMPTS");
        if(attempts instanceof Map<?,?> map)map.clear();
        clock.current=LocalDateTime.of(2026,10,27,8,0).atZone(clock.getZone()).toInstant();
        for(HttpClient c:List.of(member,coach,other,manager,cashier))call(c,"GET","/api/auth/me",null,200);
    }
    int registration(String key) throws Exception {
        var r=call(member,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey",key),200);
        int id=r.path("id").asInt();call(coach,"POST","/api/advanced-registrations/"+id+"/decision",Map.of("accepted",true),200);return id;
    }
    int paid(String key) throws Exception {
        var order=call(member,"POST","/api/advanced-registrations/"+registration(key)+"/order",Map.of("paymentMethod","Cash"),200);
        call(cashier,"POST","/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash",null,200);
        return order.path("subscriptionId").asInt();
    }
    Map<String,Object> offer(int sub,String key,int day) {
        return Map.of("subscriptionId",sub,"coachId",coachId,"roomId",roomId,"subjectId",subjectId,
                "sessionDate",LocalDate.of(2026,11,day).toString(),"startTime","06:00","endTime","07:30","agreementConfirmed",true,"requestKey",key);
    }
    int balance(int sub){return jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sub);}
    int countSessions(int sub){return jdbc.queryForObject("SELECT count(*) FROM sessions WHERE subscription_id=?",Integer.class,sub);}
    JsonNode respond(HttpClient who,int id,boolean accepted,int expected)throws Exception{return call(who,"POST","/api/private-session-proposals/"+id+"/respond",Map.of("accepted",accepted),expected);}

    @Test void privateClassAppearsOnlyAfterCoachAcceptanceAndIsScopedToItsOwner() throws Exception {
        int before=jdbc.queryForObject("SELECT count(*) FROM classes WHERE class_type='Advanced'",Integer.class);
        var requested=call(member,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey","class-owner-request"),200);
        int registration=requested.path("id").asInt();assertTrue(requested.path("classId").isNull());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM classes WHERE class_type='Advanced'",Integer.class));
        var accepted=call(coach,"POST","/api/advanced-registrations/"+registration+"/decision",Map.of("accepted",true),200);
        int course=accepted.path("classId").asInt();assertTrue(course>0);
        assertEquals(before+1,jdbc.queryForObject("SELECT count(*) FROM classes WHERE class_type='Advanced'",Integer.class));
        assertEquals(course,call(coach,"POST","/api/advanced-registrations/"+registration+"/decision",Map.of("accepted",true),200).path("classId").asInt());
        var own=call(member,"GET","/api/classes/"+course,null,200);
        assertEquals(memberId,own.path("memberId").asInt());assertEquals(coachId,own.path("coachId").asInt());
        assertEquals("AwaitingConfirmation",own.path("advancedState").asText());assertNotEquals("Open",own.path("status").asText());
        assertEquals(1,own.path("enrolledCount").asInt());assertNull(jdbc.queryForObject("SELECT subscription_id FROM advanced_registrations WHERE id=?",Integer.class,registration));
        call(other,"GET","/api/classes/"+course,null,404);
        for(var row:call(other,"GET","/api/classes",null,200))assertNotEquals(course,row.path("id").asInt());
        var rejected=call(member,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey","class-owner-rejected"),200);
        call(coach,"POST","/api/advanced-registrations/"+rejected.path("id").asInt()+"/decision",Map.of("accepted",false),200);
        assertEquals(before+1,jdbc.queryForObject("SELECT count(*) FROM classes WHERE class_type='Advanced'",Integer.class));
    }

    @Test void subscriptionsOfTheSameSubjectNeverShareAClassOrAcceptAnotherClassId() throws Exception {
        int first=paid("class-course-one"),second=paid("class-course-two");
        int classOne=jdbc.queryForObject("SELECT class_id FROM advanced_registrations WHERE subscription_id=?",Integer.class,first);
        int classTwo=jdbc.queryForObject("SELECT class_id FROM advanced_registrations WHERE subscription_id=?",Integer.class,second);
        assertNotEquals(classOne,classTwo);
        var body=new java.util.HashMap<String,Object>(offer(first,"class-cross-owner",28));body.put("classId",classTwo);
        assertEquals("ADVANCED_CLASS_MISMATCH",call(coach,"POST","/api/private-session-proposals",body,400).path("code").asText());
        assertEquals(12,balance(first));assertEquals(0,countSessions(first));
        body.put("classId",classOne);
        int proposal=call(coach,"POST","/api/private-session-proposals",body,201).path("id").asInt();
        int session=respond(member,proposal,true,200).path("sessionId").asInt();
        assertEquals(classOne,jdbc.queryForObject("SELECT class_id FROM sessions WHERE session_id=?",Integer.class,session));
        assertEquals(11,balance(first));assertEquals(12,balance(second));
        call(member,"POST","/api/sessions/"+session+"/cancel-private",null,200);assertEquals(12,balance(first));
    }

    @Test void confirmationExpiresAtExactly24HoursWithoutIssuingInvoice() throws Exception {
        int id=registration("confirm-expired");
        int before=jdbc.queryForObject("SELECT count(*) FROM invoices",Integer.class);
        var accepted=call(member,"GET","/api/advanced-registrations",null,200);
        assertTrue(accepted.toString().contains("confirmationDeadline"));
        clock.advance(Duration.ofHours(24));
        var closed=registrationView(member,id);assertFalse(closed.path("purchaseConfirmationAllowed").asBoolean());assertEquals("REGISTRATION_CONFIRMATION_EXPIRED",closed.path("purchaseConfirmationBlockReason").asText());assertTrue(closed.path("invoiceId").isNull());
        assertTrue(call(member,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod","Cash"),409).toString().contains("REGISTRATION_CONFIRMATION_EXPIRED"));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM invoices",Integer.class));
        assertNull(jdbc.queryForObject("SELECT subscription_id FROM advanced_registrations WHERE id=?",Integer.class,id));
    }
    @Test void confirmationBeforeDeadlineStartsSeparateFiveMinuteInvoiceAndRetryDoesNotExtend()throws Exception {
        int id=registration("confirm-before");clock.advance(Duration.ofHours(24).minusSeconds(1));
        var first=call(member,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod","VNPay"),200);
        var due=LocalDateTime.parse(first.path("expiresAt").asText());assertEquals(LocalDateTime.now(clock).plusMinutes(5),due);
        clock.advance(Duration.ofSeconds(2));
        var retry=call(member,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod","VNPay"),200);
        assertEquals(first.path("invoiceId"),retry.path("invoiceId"));assertEquals(first.path("expiresAt"),retry.path("expiresAt"));
        assertEquals(0,balance(first.path("subscriptionId").asInt()));
    }
    @Test void pendingProposalReservesNothingAndOnlyOwnerCanAcceptOnce()throws Exception {
        int sub=paid("proposal-owner");var body=offer(sub,"owner-slot",10);
        call(coach,"POST","/api/sessions/book-private",body,400);
        call(member,"POST","/api/private-session-proposals",body,403);
        var p=call(coach,"POST","/api/private-session-proposals",body,201);int id=p.path("id").asInt();
        assertEquals("Pending",p.path("status").asText());assertTrue(p.path("sessionId").isNull());assertEquals(12,balance(sub));assertEquals(0,countSessions(sub));
        assertEquals(id,call(coach,"POST","/api/private-session-proposals",body,201).path("id").asInt());
        call(other,"GET","/api/private-session-proposals/"+id,null,403);respond(other,id,true,403);respond(coach,id,true,403);
        var accepted=respond(member,id,true,200);int session=accepted.path("sessionId").asInt();
        assertTrue(session>0);assertEquals("Accepted",accepted.path("status").asText());assertEquals("Scheduled",accepted.path("session").path("status").asText());
        assertEquals(11,balance(sub));assertEquals(1,countSessions(sub));assertEquals(session,respond(member,id,true,200).path("sessionId").asInt());
        respond(member,id,false,409);assertEquals(11,balance(sub));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='ACCEPT_PRIVATE_SESSION' AND target_id=?",Integer.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PRIVATE_BOOKING_CONFIRMED:"+session));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='ACCEPT_PRIVATE_SESSION' AND target_id=? AND center_id=1 AND audit_category='BUSINESS'",Integer.class,id));
    }
    @Test void rejectionAndExpiredProposalNeverConsumeCredit()throws Exception {
        int sub=paid("reject-expire");var body=offer(sub,"reject",11);
        int rejected=call(coach,"POST","/api/private-session-proposals",body,201).path("id").asInt();
        assertEquals("Rejected",respond(member,rejected,false,200).path("status").asText());respond(member,rejected,false,200);respond(member,rejected,true,409);
        int expired=call(coach,"POST","/api/private-session-proposals",offer(sub,"expire",12),201).path("id").asInt();
        clock.current=LocalDateTime.of(2026,11,11,18,0,1).atZone(clock.getZone()).toInstant();
        assertEquals("Expired",call(member,"GET","/api/private-session-proposals/"+expired,null,200).path("status").asText());respond(member,expired,true,409);
        assertEquals(12,balance(sub));assertEquals(0,countSessions(sub));
    }
    @Test void acceptanceAtExactlyTwelveHoursRechecksCreditAndResources()throws Exception {
        int[] spareRoom={0}; tx.executeWithoutResult(t->{var r=new Room();r.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON);r.setName("Revalidation room");r.setCapacity(15);r.setStatus("Available");r.setCenterId(1);r.setFacilityType("Indoor");spareRoom[0]=rooms.save(r).getId();});
        int sub=paid("revalidate");var body=new HashMap<String,Object>(offer(sub,"revalidate",13));body.put("roomId",spareRoom[0]);int p=call(coach,"POST","/api/private-session-proposals",body,201).path("id").asInt();
        tx.executeWithoutResult(t->rooms.findById(spareRoom[0]).orElseThrow().setStatus("Maintenance"));
        try{respond(member,p,true,400);assertEquals(0,countSessions(sub));assertEquals(12,balance(sub));}finally{tx.executeWithoutResult(t->rooms.findById(spareRoom[0]).orElseThrow().setStatus("Available"));}
        tx.executeWithoutResult(t->subscriptions.findById(sub).orElseThrow().setRemainingCredit(0));
        try{respond(member,p,true,400);assertEquals(0,countSessions(sub));}finally{tx.executeWithoutResult(t->subscriptions.findById(sub).orElseThrow().setRemainingCredit(12));}
        clock.current=LocalDateTime.of(2026,11,12,18,0).atZone(clock.getZone()).toInstant();
        respond(member,p,true,200);assertEquals(11,balance(sub));assertEquals(1,countSessions(sub));
    }
    @Test void concurrentOwnerAcceptsDeductOnceAndCompetingProposalsDoNotHoldResources()throws Exception {
        int sub=paid("accept-races");int id=call(coach,"POST","/api/private-session-proposals",offer(sub,"same",14),201).path("id").asInt();
        acceptRace(new int[]{id,id},List.of(200,200));assertEquals(11,balance(sub));assertEquals(1,countSessions(sub));
        int a=call(coach,"POST","/api/private-session-proposals",offer(sub,"compete-a",15),201).path("id").asInt();
        int b=call(coach,"POST","/api/private-session-proposals",offer(sub,"compete-b",15),201).path("id").asInt();
        assertEquals(11,balance(sub));acceptRace(new int[]{a,b},List.of(200,409));assertEquals(10,balance(sub));assertEquals(2,countSessions(sub));
    }
    @Test void immutableProposalAndRevokedCoachCannotBookOnMemberResponse()throws Exception {
        int sub=paid("proposer-revalidation");var body=offer(sub,"immutable",16);
        int id=call(coach,"POST","/api/private-session-proposals",body,201).path("id").asInt();
        var different=new HashMap<>(body);different.put("startTime","07:45");different.put("endTime","09:15");
        call(coach,"POST","/api/private-session-proposals",different,409);
        tx.executeWithoutResult(t->users.findById(coachId).orElseThrow().setStatus("Inactive"));
        try{respond(member,id,true,403);assertEquals(0,countSessions(sub));assertEquals(12,balance(sub));}
        finally{tx.executeWithoutResult(t->users.findById(coachId).orElseThrow().setStatus("Active"));}
        tx.executeWithoutResult(t->members.findById(memberId).orElseThrow().setBookingBlockedUntil(LocalDateTime.now(clock).plusDays(1)));
        try{respond(member,id,true,400);assertEquals(0,countSessions(sub));assertEquals(12,balance(sub));}
        finally{tx.executeWithoutResult(t->members.findById(memberId).orElseThrow().setBookingBlockedUntil(null));}
    }
    @Test void crossCenterAndReceptionCannotInspectOrRespondToProposal()throws Exception {
        int sub=paid("scope");int id=call(coach,"POST","/api/private-session-proposals",offer(sub,"scope-slot",17),201).path("id").asInt();
        call(cashier,"GET","/api/private-session-proposals",null,403);
        tx.executeWithoutResult(t->{if(System.getProperty("scms.phases.pg-url","").isBlank())jdbc.update("INSERT INTO centers(center_id) VALUES(2)");else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(2,'Consent local B')");users.findById(otherId).orElseThrow().setCenterId(2);});
        try{
            call(other,"GET","/api/private-session-proposals/"+id,null,403);respond(other,id,true,403);
            assertEquals(0,call(other,"GET","/api/private-session-proposals",null,200).size());
        }finally{tx.executeWithoutResult(t->users.findById(otherId).orElseThrow().setCenterId(1));}
        assertEquals(12,balance(sub));assertEquals(0,countSessions(sub));
    }
    @Test void acceptedRetryAfterCancellationDoesNotRebookOrConsumeCredit()throws Exception {
        int sub=paid("accepted-history");int id=call(coach,"POST","/api/private-session-proposals",offer(sub,"history",18),201).path("id").asInt();
        int session=respond(member,id,true,200).path("sessionId").asInt();
        call(member,"POST","/api/sessions/"+session+"/cancel-private",null,200);assertEquals(12,balance(sub));
        clock.advance(Duration.ofDays(30));var retry=respond(member,id,true,200);
        assertEquals(session,retry.path("sessionId").asInt());assertEquals("Cancelled",retry.path("session").path("status").asText());
        assertEquals(12,balance(sub));assertEquals(1,countSessions(sub));
    }
    @Test void publishedContractExposesConfirmationAndClosesDirectBooking()throws Exception {
        var spec=call(member,"GET","/v3/api-docs",null,200);
        String export=System.getProperty("scms.advanced.openapi");
        if(export!=null)java.nio.file.Files.writeString(java.nio.file.Path.of(export),spec.toPrettyString());
        assertTrue(spec.path("paths").has("/api/private-session-proposals/{id}/respond"));
        assertTrue(spec.path("components").path("schemas").path("AdvancedRegistrationResponse").path("properties").has("confirmationDeadline"));
        var old=spec.path("paths").path("/api/sessions/book-private").path("post");
        assertTrue(old.path("deprecated").asBoolean());assertTrue(old.path("responses").has("400"));assertFalse(old.path("responses").has("201"));
        String output=System.getProperty("scms.openapi-output","");
        if(!output.isBlank()){
            var target=java.nio.file.Path.of(output);java.nio.file.Files.createDirectories(target.toAbsolutePath().getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(target.toFile(),spec);
        }
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void earlyPaidAttendanceAndZeroCreditKeepOriginalEntitlementAndBlockRefund(boolean bulk) throws Exception {
        int sub=paid("early-paid-"+bulk);
        int proposal=call(coach,"POST","/api/private-session-proposals",offer(sub,"early-paid-slot-"+bulk,bulk?22:19),201).path("id").asInt();
        int session=respond(member,proposal,true,200).path("sessionId").asInt();
        var expiry=jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,sub);
        Map<String,Object> body=bulk?Map.of("sessionId",session,"coachId",coachId,"items",List.of(Map.of("memberId",memberId,"attendanceStatus","Present")))
                :Map.of("sessionId",session,"memberId",memberId,"coachId",coachId,"attendanceStatus","Present");
        String route="/api/training-results"+(bulk?"/bulk":"");
        assertEquals("ATTENDANCE_FORBIDDEN",call(member,"POST",route,body,403).path("code").asText());
        assertEquals("ATTENDANCE_WINDOW_CLOSED",call(coach,"POST",route,body,400).path("code").asText());
        early(true); jdbc.update("UPDATE member_packages SET remaining_credit=0 WHERE subscription_id=?",sub);
        assertTrue(call(coach,"GET","/api/sessions/"+session,null,200).path("attendanceAllowed").asBoolean());
        call(coach,"POST",route,body,200); call(coach,"POST",route,body,200);
        var memberView=call(member,"GET","/api/sessions/"+session,null,200);
        assertFalse(memberView.path("cancellationAllowed").asBoolean());assertEquals("SESSION_ALREADY_ATTENDED",memberView.path("cancellationBlockReason").asText());
        var denied=call(member,"POST","/api/sessions/"+session+"/cancel-private",null,409);
        assertEquals("SESSION_ALREADY_ATTENDED",denied.path("code").asText());
        assertEquals("Buổi học đã được điểm danh, không thể hủy hoàn credit.",denied.path("message").asText());
        assertEquals(0,balance(sub)); assertEquals(expiry,jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,sub));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=? AND state='Present' AND superseded_at IS NULL",Integer.class,session));
        assertEquals("Scheduled",jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,session));
        assertEquals(LocalDateTime.now(clock),jdbc.queryForObject("SELECT check_in_time FROM attendances WHERE session_id=?",java.sql.Timestamp.class,session).toLocalDateTime());
        proof("D15-EARLY-ZERO-CREDIT","D03,D04,D05,D15","Paid + Member Accepted, 0 remaining credit","Recorded once; cancellation409; credit/expiry unchanged",cancellationSql(),session);
    }
    @Test void earlyQrUsesSameWindowEligibilityAndCancellationGuard() throws Exception {
        int sub=paid("early-qr"); int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"early-qr-slot",20),201).path("id").asInt();
        int session=respond(member,p,true,200).path("sessionId").asInt();
        var code=call(member,"GET","/api/attendances/checkin-code",null,200).path("payload").asText();
        var body=Map.of("sessionId",session,"memberId",memberId,"code",code);
        call(coach,"POST","/api/attendances/scan",body,400); early(true);
        call(coach,"POST","/api/attendances/scan",body,200);
        assertEquals("SESSION_ALREADY_ATTENDED",call(member,"POST","/api/sessions/"+session+"/cancel-private",null,409).path("code").asText());
        assertEquals(11,balance(sub));
    }
    @Test void concurrentCancellationAndEarlyAttendanceHaveOnlyOneCommittedWinner() throws Exception {
        int sub=paid("early-cancel-race"); int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"early-race-slot",21),201).path("id").asInt();
        int session=respond(member,p,true,200).path("sessionId").asInt(); early(true);
        var start=new CountDownLatch(1); var ready=new CountDownLatch(2); var pool=Executors.newFixedThreadPool(2); int from=exchanges.size();
        try (var blocker=dataSource.getConnection()) {
            boolean pg=!System.getProperty("scms.phases.pg-url","").isBlank();
            if(pg) { blocker.setAutoCommit(false);try(var statement=blocker.createStatement()){statement.executeQuery("SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE").close();} }
            var cancel=pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(member,"POST","/api/sessions/"+session+"/cancel-private",null,0);});
            var record=pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(coach,"POST","/api/training-results",Map.of("sessionId",session,"memberId",memberId,"coachId",coachId,"attendanceStatus","Absent"),0);});
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            if(pg) {
                long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10); int waiting=0;
                while(System.nanoTime()<until) {
                    waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'",Integer.class);
                    if(waiting>=2)break;Thread.sleep(25);
                }
                assertTrue(waiting>=2,"Both HTTP transactions must reach the database lock");blocker.rollback();
            }
            cancel.get(30,TimeUnit.SECONDS);record.get(30,TimeUnit.SECONDS);
            var statuses=exchanges.subList(from,exchanges.size()).stream().map(e->(Integer)e.get("actualStatus")).sorted().toList();
            assertTrue(statuses.equals(List.of(200,400)) || statuses.equals(List.of(200,409)),statuses.toString());
        } finally { start.countDown();pool.shutdownNow(); }
        String state=jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,session);
        int attendance=jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=? AND superseded_at IS NULL",Integer.class,session);
        if ("Cancelled".equals(state)) { assertEquals(12,balance(sub));assertEquals(0,attendance); }
        else {assertEquals("Scheduled",state);assertEquals(11,balance(sub));assertEquals(1,attendance);}
        proof("D05-CANCEL-ATTENDANCE-RACE","D04,D05,D15","Two authenticated requests released by one latch","Only attendance or credit refund commits",cancellationSql(),session);
    }
    @Test void registrationLinksItsOriginalInvoiceAndGetDoesNotChangeIt()throws Exception {
        int id=call(member,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey","metadata-invoice"),200).path("id").asInt();
        var requested=registrationView(member,id);
        assertFalse(requested.path("purchaseConfirmationAllowed").asBoolean());assertEquals("COACH_ACCEPTANCE_REQUIRED",requested.path("purchaseConfirmationBlockReason").asText());
        assertTrue(requested.path("invoiceId").isNull());assertTrue(requested.path("invoiceStatus").isNull());
        call(coach,"POST","/api/advanced-registrations/"+id+"/decision",Map.of("accepted",true),200);
        assertTrue(registrationView(member,id).path("purchaseConfirmationAllowed").asBoolean());
        assertEquals("REGISTRATION_OWNER_REQUIRED",registrationView(coach,id).path("purchaseConfirmationBlockReason").asText());
        clock.advance(Duration.ofHours(24).minusSeconds(1));
        var order=call(member,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod","Cash"),200);int invoice=order.path("invoiceId").asInt();
        var before=jdbc.queryForList("SELECT * FROM invoices WHERE invoice_id=?",invoice);
        clock.advance(Duration.ofHours(1));
        var linked=registrationView(member,id);assertEquals(invoice,linked.path("invoiceId").asInt());assertEquals("Pending",linked.path("invoiceStatus").asText());
        assertTrue(linked.path("purchaseConfirmationAllowed").asBoolean(),"Existing order retry is allowed after the new-order cutoff");
        assertTrue(linked.path("serverTime").asText().endsWith("+07:00"));
        var own=call(member,"GET","/api/members/me/invoices/"+invoice,null,200);assertEquals(invoice,own.path("invoiceId").asInt());assertTrue(own.path("serverTime").asText().endsWith("+07:00"));
        call(other,"GET","/api/members/me/invoices/"+invoice,null,404);
        assertEquals(before,jdbc.queryForList("SELECT * FROM invoices WHERE invoice_id=?",invoice));
        var retry=call(member,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod","Cash"),200);
        assertEquals(invoice,retry.path("invoiceId").asInt());assertEquals(order.path("expiresAt"),retry.path("expiresAt"));assertEquals(order.path("paymentMethod"),retry.path("paymentMethod"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE subscription_id=?",Integer.class,order.path("subscriptionId").asInt()));
    }
    JsonNode registrationView(HttpClient actor,int id)throws Exception {
        for(var row:call(actor,"GET","/api/advanced-registrations",null,200))if(row.path("id").asInt()==id)return row;
        throw new AssertionError("Registration missing from own scope");
    }
    @Test void supersededAttendanceDoesNotBlockRefundAndCompletedHasItsOwnCode()throws Exception {
        int sub=paid("history-cancel-metadata");int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"history-cancel-metadata-slot",23),201).path("id").asInt();
        var accepted=respond(member,p,true,200);int session=accepted.path("sessionId").asInt();
        assertTrue(accepted.path("session").path("cancellationAllowed").asBoolean());
        assertFalse(accepted.path("session").path("attendanceAllowed").asBoolean());
        assertTrue(accepted.path("session").path("serverTime").asText().endsWith("+07:00"));
        tx.executeWithoutResult(t->{var a=new com.sportscenter.training.Attendance();a.setSession(sessionRepository.findById(session).orElseThrow());a.setMember(users.findById(memberId).orElseThrow());a.setRecordedBy(users.findById(coachId).orElseThrow());a.setState("Present");a.setSupersededAt(LocalDateTime.now(clock));attendanceRepository.save(a);});
        assertTrue(call(member,"GET","/api/sessions/"+session,null,200).path("cancellationAllowed").asBoolean());
        call(member,"POST","/api/sessions/"+session+"/cancel-private",null,200);assertEquals(12,balance(sub));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=? AND superseded_at IS NOT NULL",Integer.class,session));
        var second=offer(sub,"completed-code-slot",24);int next=respond(member,call(coach,"POST","/api/private-session-proposals",second,201).path("id").asInt(),true,200).path("sessionId").asInt();
        jdbc.update("UPDATE sessions SET status='Completed' WHERE session_id=?",next);
        assertEquals("SESSION_COMPLETED",call(member,"GET","/api/sessions/"+next,null,200).path("cancellationBlockReason").asText());
        assertEquals("SESSION_COMPLETED",call(member,"POST","/api/sessions/"+next+"/cancel-private",null,400).path("code").asText());assertEquals(11,balance(sub));
    }
    @Test void newInvoiceHasAtMostThirtyContinuousMinutesAndExistingMoneyAndDeadlineStayFrozen()throws Exception {
        clock.current=LocalDateTime.of(2026,10,27,10,50).atZone(clock.getZone()).toInstant();
        org.springframework.test.util.ReflectionTestUtils.setField(paymentProperties,"expireMinutes",120);
        int registration=registration("invoice-thirty");
        var order=call(member,"POST","/api/advanced-registrations/"+registration+"/order",Map.of("paymentMethod","Cash"),200);
        int invoice=order.path("invoiceId").asInt(),sub=order.path("subscriptionId").asInt();
        var due=LocalDateTime.of(2026,10,27,11,20);
        assertEquals(due,LocalDateTime.parse(order.path("expiresAt").asText()));
        assertEquals(due,jdbc.queryForObject("SELECT expires_at FROM invoices WHERE invoice_id=?",java.sql.Timestamp.class,invoice).toLocalDateTime());
        jdbc.update("UPDATE subjects SET advanced_price=advanced_price+1000 WHERE subject_id=?",subjectId);
        try {
            var own=call(member,"GET","/api/members/me/invoices/"+invoice,null,200);
            assertEquals(0,new BigDecimal("3000000").compareTo(own.path("amount").decimalValue()));
            assertEquals(0,new BigDecimal("3000000").compareTo(jdbc.queryForObject("SELECT purchase_amount FROM member_packages WHERE subscription_id=?",BigDecimal.class,sub)));
            clock.current=due.atZone(clock.getZone()).toInstant();
            call(cashier,"POST","/api/invoices/"+invoice+"/confirm-cash",null,409);assertEquals(0,balance(sub));
            assertNotEquals("Active",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,sub));
            var retry=call(member,"POST","/api/advanced-registrations/"+registration+"/order",Map.of("paymentMethod","Cash"),200);
            assertEquals(invoice,retry.path("invoiceId").asInt());assertEquals(order.path("expiresAt"),retry.path("expiresAt"));assertEquals("Cash",retry.path("paymentMethod").asText());
            assertTrue(invoiceService.expireInvoice(invoice));
            own=call(member,"GET","/api/members/me/invoices/"+invoice,null,200);assertEquals("Expired",own.path("paymentStatus").asText());
            assertEquals(due,LocalDateTime.parse(own.path("expiresAt").asText()));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE subscription_id=?",Integer.class,sub));
        } finally {jdbc.update("UPDATE subjects SET advanced_price=advanced_price-1000 WHERE subject_id=?",subjectId);}
        proof("INVOICE-30-CONTINUOUS","D02,D03,D10","New Cash order10:50 with configuration120 clamped to30","11:20 deadline across lunch; at expiry no activation; old amount/method/deadline retained",rightsSql(),sub);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints={299,300})
    void issuedQrHasFullTtlRefreshAndStrictExpiryWithoutConsumingOnDeniedWrites(int age)throws Exception {
        int sub=paid("issued-ttl-"+age);int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"issued-ttl-slot-"+age,age==299?25:26),201).path("id").asInt();
        int session=respond(member,p,true,200).path("sessionId").asInt();early(true);
        var issued=LocalDateTime.of(2026,10,27,20,40,17);clock.current=issued.atZone(clock.getZone()).toInstant();
        var first=call(member,"GET","/api/attendances/checkin-code",null,200);assertEquals(300,first.path("expiresInSeconds").asInt());
        assertEquals(issued.plusSeconds(300),OffsetDateTime.parse(first.path("expiresAt").asText()).toLocalDateTime());
        String code=first.path("payload").asText();var body=Map.of("sessionId",session,"memberId",memberId,"code",code);
        call(member,"POST","/api/attendances/scan",body,403);
        jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE subscription_id=?",sub);
        try {call(coach,"POST","/api/attendances/scan",body,400);assertNull(jdbc.queryForObject("SELECT checkin_code_used_at FROM members WHERE user_id=?",java.sql.Timestamp.class,memberId));}
        finally {jdbc.update("UPDATE invoices SET reconciliation_required=false WHERE subscription_id=?",sub);}
        clock.current=issued.plusSeconds(43).atZone(clock.getZone()).toInstant();
        var refresh=call(member,"GET","/api/attendances/checkin-code",null,200);assertEquals(257,refresh.path("expiresInSeconds").asInt());
        assertEquals(first.path("expiresAt"),refresh.path("expiresAt"));assertTrue(java.security.MessageDigest.isEqual(code.getBytes(),refresh.path("payload").asText().getBytes()),"Refresh must keep the issuance");
        clock.current=issued.plusSeconds(age).atZone(clock.getZone()).toInstant();
        call(coach,"POST","/api/attendances/scan",body,age==299?200:400);
        var used=jdbc.queryForObject("SELECT checkin_code_used_at FROM members WHERE user_id=?",java.sql.Timestamp.class,memberId);
        if(age==299)assertEquals(issued.plusSeconds(age),used.toLocalDateTime());else assertNull(used);
        call(coach,"POST","/api/attendances/scan",body,400);
        assertEquals(age==299?1:0,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=? AND superseded_at IS NULL",Integer.class,session));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM training_results WHERE session_id=?",Integer.class,session));assertEquals(11,balance(sub));
        var next=call(member,"GET","/api/attendances/checkin-code",null,200);assertEquals(300,next.path("expiresInSeconds").asInt());
        assertFalse(java.security.MessageDigest.isEqual(code.getBytes(),next.path("payload").asText().getBytes()),"Used/expired issuance must not be recreated");
        call(coach,"POST","/api/attendances/scan",body,400);
        proof("QR-TTL-"+age,"D03,D04,D15,D17","Issued20:40:17, refreshed20:41, scan at age"+age,"300 from issuance, refresh257; strict expiry/replay; denied request does not consume",cancellationSql(),session);
    }
    void awaitLockWaiters(int required)throws Exception {
        boolean pg=!System.getProperty("scms.phases.pg-url","").isBlank();
        String query=pg?"SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'":"SELECT count(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL";
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);int count=0;
        while(System.nanoTime()<until) {count=jdbc.queryForObject(query,Integer.class);if(count>=required)return;Thread.sleep(25);}
        fail("Expected HTTP transactions to reach the DB lock; observed "+count);
    }
    @Test void concurrentScansConsumeOneIssuanceAndCommitOneAttendance()throws Exception {
        int sub=paid("issued-race");int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"issued-race-slot",27),201).path("id").asInt();
        int session=respond(member,p,true,200).path("sessionId").asInt();early(true);
        String code=call(member,"GET","/api/attendances/checkin-code",null,200).path("payload").asText();
        var body=Map.of("sessionId",session,"memberId",memberId,"code",code);var pool=Executors.newFixedThreadPool(2);
        int from=exchanges.size();try(var blocker=dataSource.getConnection()) {
            blocker.setAutoCommit(false);try(var st=blocker.createStatement()){st.executeQuery("SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE").close();}
            var a=pool.submit(()->call(coach,"POST","/api/attendances/scan",body,0));var b=pool.submit(()->call(coach,"POST","/api/attendances/scan",body,0));
            awaitLockWaiters(2);blocker.rollback();a.get(30,TimeUnit.SECONDS);b.get(30,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertEquals(List.of(200,400),exchanges.subList(from,exchanges.size()).stream().map(e->(Integer)e.get("actualStatus")).sorted().toList());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=?",Integer.class,session));
        assertNotNull(jdbc.queryForObject("SELECT checkin_code_used_at FROM members WHERE user_id=?",java.sql.Timestamp.class,memberId));assertEquals(11,balance(sub));
        proof("QR-CONCURRENT-SCANS","D15,D17","Two authenticated scans held at the center row before release","Exactly one200, one400; one attendance; one usedAt; unchanged credit",cancellationSql(),session);
    }
    @Test void scanWaitingForMemberLockRechecksTheSessionEndBeforeConsuming()throws Exception {
        int sub=paid("issued-end-wait");int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"issued-end-slot",28),201).path("id").asInt();
        int session=respond(member,p,true,200).path("sessionId").asInt();
        clock.current=LocalDateTime.of(2026,11,28,7,29,59).atZone(clock.getZone()).toInstant();
        String code=call(member,"GET","/api/attendances/checkin-code",null,200).path("payload").asText();
        var body=Map.of("sessionId",session,"memberId",memberId,"code",code);var pool=Executors.newSingleThreadExecutor();
        try(var blocker=dataSource.getConnection()) {
            blocker.setAutoCommit(false);try(var st=blocker.createStatement()){st.executeQuery("SELECT user_id FROM members WHERE user_id="+memberId+" FOR UPDATE").close();}
            var scan=pool.submit(()->call(coach,"POST","/api/attendances/scan",body,400));awaitLockWaiters(1);
            clock.current=LocalDateTime.of(2026,11,28,7,30).atZone(clock.getZone()).toInstant();blocker.rollback();
            assertEquals("ATTENDANCE_WINDOW_CLOSED",scan.get(30,TimeUnit.SECONDS).path("code").asText());
        } finally {pool.shutdownNow();}
        assertNull(jdbc.queryForObject("SELECT checkin_code_used_at FROM members WHERE user_id=?",java.sql.Timestamp.class,memberId));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=?",Integer.class,session));assertEquals(11,balance(sub));
        proof("QR-END-AFTER-LOCK","D15,D17","Scan passes initial window at07:29:59, waits Member, then Clock reaches07:30","400, no code consumption, attendance or credit write",cancellationSql(),session);
    }
    @AfterAll void exportCheckedEvidence()throws Exception {
        String output=System.getProperty("scms.advanced.evidence","");if(output.isBlank())return;
        var safe=json.valueToTree(exchanges);sanitizeEvidence(safe);
        java.nio.file.Files.writeString(java.nio.file.Path.of(output),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("scope","Synthetic local HTTP, no provider/FE acceptance","exchanges",safe,"acceptance",acceptance)));
    }
    static void sanitizeEvidence(JsonNode node) {
        if(node.isObject()) {
            var object=(com.fasterxml.jackson.databind.node.ObjectNode)node;
            if(object.has("path") && object.path("path").asText().equals("/v3/api-docs"))object.put("responseBody","Schema checked separately");
            var names=new ArrayList<String>();node.fieldNames().forEachRemaining(names::add);
            for(String name:names) {
                if(List.of("payload","password","otp","checkinSecret","checkinCodeNonce","paymentUrl").contains(name))object.put(name,"[redacted]");
                else sanitizeEvidence(node.get(name));
            }
        } else if(node.isArray())for(var item:node)sanitizeEvidence(item);
    }
    void acceptRace(int[] ids,List<Integer> expected)throws Exception {
        var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
        int from=exchanges.size();
        try{
            var futures=new ArrayList<Future<JsonNode>>();
            for(int id:ids)futures.add(pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return respond(member,id,true,0);}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();for(var future:futures)future.get(30,TimeUnit.SECONDS);
            assertEquals(expected,exchanges.subList(from,exchanges.size()).stream().map(e->(Integer)e.get("actualStatus")).sorted().toList());
        }finally{start.countDown();pool.shutdownNow();}
    }
}
