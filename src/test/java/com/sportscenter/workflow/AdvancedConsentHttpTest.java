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
            r.add("spring.jpa.hibernate.ddl-auto",()->"none");
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
    @Autowired com.sportscenter.training.AttendanceFinalizationProcessor finalizer;
    @Autowired SessionRepository sessionRepository;
    @Autowired com.sportscenter.training.AttendanceRepository attendanceRepository;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired MutableClock clock;
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
            var template=new com.sportscenter.sportclass.SportsClass();template.setName("Phase Advanced template");template.setSubject(s);template.setCoach(c);template.setClassType("Advanced");template.setMaxCapacity(1);template.setTuitionFee(s.getAdvancedPrice());template.setStatus("Open");template.setCenterId(1);classes.save(template);
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

    @BeforeEach void resetClock() throws Exception {
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

    @Test void confirmationExpiresAtExactly24HoursWithoutIssuingInvoice() throws Exception {
        int id=registration("confirm-expired");
        int before=jdbc.queryForObject("SELECT count(*) FROM invoices",Integer.class);
        var accepted=call(member,"GET","/api/advanced-registrations",null,200);
        assertTrue(accepted.toString().contains("confirmationDeadline"));
        clock.advance(Duration.ofHours(24));
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
        int sub=paid("revalidate");int p=call(coach,"POST","/api/private-session-proposals",offer(sub,"revalidate",13),201).path("id").asInt();
        tx.executeWithoutResult(t->rooms.findById(roomId).orElseThrow().setStatus("Maintenance"));
        try{respond(member,p,true,400);assertEquals(0,countSessions(sub));assertEquals(12,balance(sub));}finally{tx.executeWithoutResult(t->rooms.findById(roomId).orElseThrow().setStatus("Available"));}
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
        tx.executeWithoutResult(t->{jdbc.update("INSERT INTO centers(center_id) VALUES(2)");users.findById(otherId).orElseThrow().setCenterId(2);});
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
