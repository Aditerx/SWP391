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
        "spring.datasource.url=jdbc:h2:mem:phase_workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false", "spring.sql.init.mode=never",
        "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=phase-workflow-test-secret", "payment.expire-minutes=5",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "logging.level.root=WARN", "debug=false"})
@Import(PhaseWorkflowHttpTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PhaseWorkflowHttpTest {
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
    @RegisterExtension final TestWatcher dependencyGuard=new TestWatcher() {
        @Override public void testFailed(ExtensionContext context,Throwable cause){precedingWorkflowFailed=true;}
    };

    @BeforeEach void scenario(TestInfo info) throws Exception {
        Assumptions.assumeFalse(precedingWorkflowFailed,"BLOCKED: a preceding committed fixture workflow failed");
        currentScenario=info.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
        caseStart=exchanges.size();
        for(HttpClient client:List.of(member,coach,manager,cashier,other)) {
            var me=call(client,"GET","/api/auth/me",null,200);
            assertEquals(actors.get(client)+"@example.test",me.path("email").asText());
        }
    }

    @BeforeAll void setup() throws Exception {
        tx.executeWithoutResult(t->{
            jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 10000");
            if(jdbc.queryForObject("SELECT count(*) FROM centers WHERE center_id=1",Integer.class)==0) jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            Role mr=role("Member"), cr=role("Coach","RECORD_RESULT"), mgr=role("CenterManager","MANAGE_CLASSES","VIEW_REPORTS","MANAGE_INVOICES","MANAGE_SUBSCRIPTIONS"), rr=role("Receptionist","MANAGE_INVOICES");
            User m=user("phase.member",mr),c=user("phase.coach",cr); memberId=m.getId(); coachId=c.getId();
            otherId=user("phase.other",mr).getId(); user("phase.manager",mgr);
            User desk=user("phase.cashier",rr); Receptionist deskProfile=new Receptionist();deskProfile.setUser(desk);receptionists.save(deskProfile);
            for(User u:List.of(m,users.findById(otherId).orElseThrow())) {Member profile=new Member();profile.setUser(u);profile.setJoinDate(LocalDate.now(clock));members.save(profile);}
            Coach profile=new Coach();profile.setUser(c);coaches.save(profile);
            Subject s=subjects.findBySlug("cau-long").orElseGet(Subject::new);s.setName("Cầu lông");s.setSlug("cau-long");s.setOutdoor(false);s.setStatus("Active");s.setNormalPrice(new BigDecimal("900000"));s.setAdvancedPrice(new BigDecimal("3000000"));subjects.save(s);subjectId=s.getId();
            Specialization spec=new Specialization();spec.setName("phase-badminton");spec.setSubject(s);spec.setStatus("Active");specs.save(spec);assignments.save(new CoachSpecialization(profile,spec));
            var template=new com.sportscenter.sportclass.SportsClass();template.setName("Phase Advanced template");template.setSubject(s);template.setCoach(c);template.setClassType("Advanced");template.setMaxCapacity(1);template.setTuitionFee(s.getAdvancedPrice());template.setStatus("Open");template.setCenterId(1);classes.save(template);
            Room r=new Room();r.setName("Phase test room");r.setCapacity(15);r.setStatus("Available");r.setCenterId(1);r.setFacilityType("Indoor");rooms.save(r);roomId=r.getId();
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
    @Test @Order(1) void acceptanceCashPriceSnapshotAndIdempotency() throws Exception {
        var order=requestOrder("cash-one","Cash",member);invoiceId=order.get("invoiceId").asInt();subscriptionId=order.get("subscriptionId").asInt();invoiceCode=order.get("invoiceCode").asText();
        assertEquals(3000000,order.get("amount").asInt());
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        proof("ADV-PENDING","D03,D10","Member A, accepted registration, Cash order before collection","Order/retry share invoice; unaccepted order rejected; Pending has 0 credit",rightsSql(),subscriptionId);
        tx.executeWithoutResult(t->{var s=subjects.findById(subjectId).orElseThrow();s.setAdvancedPrice(new BigDecimal("3100000"));});
        call(other,"GET","/api/members/me/invoices/"+invoiceId,null,404);
        call(manager,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,403);
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoiceId));
        proof("MONEY-OWNER-ROLE","D03,D10","Member B and CenterManager+MANAGE_INVOICES target Member A invoice","Other owner 404; Manager without Receptionist role 403; invoice remains Pending, credit 0","SELECT payment_status,amount FROM invoices WHERE invoice_id=?",invoiceId);
        call(cashier,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,200);
        call(cashier,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,409);
        assertEquals(new BigDecimal("3000000.00"),jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoiceId));
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        assertEquals(LocalDateTime.of(2026,12,12,8,0),jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",LocalDateTime.class,subscriptionId));
        var rights=call(member,"GET","/api/members/"+memberId+"/subscriptions",null,200).get(0);
        assertEquals("Active",rights.get("status").asText());assertEquals(12,rights.get("remainingCredit").asInt());
        assertEquals(3000000,rights.get("price").asInt());assertEquals("2026-12-12T08:00:00",rights.get("expiresAt").asText());
        call(other,"GET","/api/members/"+memberId+"/subscriptions",null,403);
        proof("CASH-SNAPSHOT-RETRY","D02,D03,D10,D12","Catalog changed 3000000 to 3100000 after order; cashier collects once","Paid amount/purchaseAmount remain 3000000; Active/12 credit; deadline 2026-12-12T08:00 including closure; retry 409; other owner rights 403",rightsSql(),subscriptionId);
    }
    @Test @Order(2) void returnCannotPayAndSignedIpnReplaysOnlyOnce() throws Exception {
        var order=requestOrder("online-one","VNPay",member);int iid=order.get("invoiceId").asInt();int sid=order.get("subscriptionId").asInt();
        String query=callback(order);
        call(member,"GET","/api/payments/vnpay/return?"+query,null,302);
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,iid));
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
        proof("VNPAY-RETURN","D03,D10","Member A VNPay order, valid local signed Return","302 only; invoice Pending, no entitlement",rightsSql(),sid);
        assertEquals("00",call(member,"GET","/api/payments/vnpay/ipn?"+query,null,200).get("RspCode").asText());
        assertEquals("02",call(member,"GET","/api/payments/vnpay/ipn?"+query,null,200).get("RspCode").asText());
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
        proof("VNPAY-IPN-RETRY","D03,D10","Same local signed IPN submitted twice","RspCode 00 then 02; one activation/12 credit",rightsSql(),sid);
    }
    @Test @Order(3) void deadlineAndLateIpnRequireReconciliationWithoutEntitlement() throws Exception {
        var order=requestOrder("late-one","VNPay",member);int iid=order.get("invoiceId").asInt(),sid=order.get("subscriptionId").asInt();
        lateInvoiceId=iid;
        clock.advance(Duration.ofMinutes(5));
        call(member,"POST","/api/invoices/"+iid+"/pay-online",null,409);
        call(member,"GET","/api/payments/vnpay/ipn?"+callback(order),null,200);
        assertTrue(jdbc.queryForObject("SELECT reconciliation_required FROM invoices WHERE invoice_id=?",Boolean.class,iid));
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,sid));
        var lateReadback=call(member,"GET","/api/members/me/invoices/"+iid,null,200);
        assertEquals("Paid",lateReadback.get("paymentStatus").asText());assertTrue(lateReadback.get("reconciliationRequired").asBoolean());
        proof("VNPAY-EXACT-CUTOFF","D03,D10","Clock advanced exactly 5 minutes after VNPay order","pay-online 409; signed IPN records Paid/reconciliationRequired, Cancelled subscription/0 credit",rightsSql(),sid);
        // A verified, on-time receipt must survive a business failure without granting unusable rights.
        var unavailable=requestOrder("on-time-unavailable","VNPay",member);
        jdbc.update("UPDATE subjects SET status='Inactive' WHERE subject_id=?",subjectId);
        try {
            assertEquals("00",call(member,"GET","/api/payments/vnpay/ipn?"+callback(unavailable),null,200).get("RspCode").asText());
            assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,unavailable.get("invoiceId").asInt()));
            assertTrue(jdbc.queryForObject("SELECT reconciliation_required FROM invoices WHERE invoice_id=?",Boolean.class,unavailable.get("invoiceId").asInt()));
            assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,unavailable.get("subscriptionId").asInt()));
        } finally { jdbc.update("UPDATE subjects SET status='Active' WHERE subject_id=?",subjectId); }
        proof("VNPAY-UNAVAILABLE","D03,D10","Subject temporarily Inactive before otherwise timely signed IPN","Money retained Paid/reconciliationRequired; no credit",rightsSql(),unavailable.get("subscriptionId").asInt());
    }
    int scheduledId;
    @Test @Order(4) void primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit() throws Exception {
        var payload=booking("book-boundary",LocalDate.of(2026,10,28),"06:00","07:30");
        call(member,"POST","/api/sessions/book-private",payload,403);
        var missing=new HashMap<>(payload);missing.put("agreementConfirmed",false);
        call(coach,"POST","/api/sessions/book-private",missing,400);
        var created=call(coach,"POST","/api/sessions/book-private",payload,201);int id=created.get("id").asInt();
        assertEquals(id,call(coach,"POST","/api/sessions/book-private",payload,201).get("id").asInt());
        assertEquals(11,credit());
        assertEquals(11,call(member,"GET","/api/members/"+memberId+"/subscriptions",null,200).get(0).get("remainingCredit").asInt());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PRIVATE_BOOKING_CONFIRMED:"+id));
        call(manager,"PATCH","/api/sessions/"+id+"/status",Map.of("status","Completed"),400);
        call(manager,"PATCH","/api/subscriptions/"+subscriptionId+"/status",Map.of("status","Expired"),400);
        assertEquals("Scheduled",jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,id));assertEquals(11,credit());
        proof("BOOK-CONSENT-RETRY","D04","Active subscription; Member and Primary Coach; same booking key/body repeated","Member 403; consent false 400; valid/retry same session, credit 11, one notification; generic status changes rejected",rightsSql(),subscriptionId);
        clock.current=LocalDateTime.of(2026,10,27,18,0).atZone(clock.getZone()).toInstant();
        call(member,"POST","/api/sessions/"+id+"/cancel-private",null,200);
        call(member,"POST","/api/sessions/"+id+"/cancel-private",null,400);
        assertEquals(12,credit());
        assertEquals(12,call(member,"GET","/api/members/"+memberId+"/subscriptions",null,200).get(0).get("remainingCredit").asInt());
        proof("CANCEL-EXACT-12H","D05","Member A cancels own 06:00 session at previous day 18:00","200 then repeated cancellation 400; exactly one credit restored to 12",cancellationSql(),id);
        var next=call(coach,"POST","/api/sessions/book-private",booking("notice-session",LocalDate.of(2026,10,28),"07:45","09:15"),201);
        scheduledId=next.get("id").asInt();
        clock.current=LocalDateTime.of(2026,10,28,0,0).atZone(clock.getZone()).toInstant();
        var notice=call(member,"POST","/api/sessions/"+scheduledId+"/absence",Map.of("reason","Unexpected personal issue"),200);
        assertTrue(notice.get("active").asBoolean());assertEquals(11,credit());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=?",Integer.class,scheduledId));
        call(member,"POST","/api/sessions/"+scheduledId+"/cancel-private",null,400);
        proof("ABSENCE-UNDER-12H","D05","Member A notice 7h45 before session; credit already consumed","Notice active; no attendance; session Scheduled; no refund, cancel 400",rightsSql(),subscriptionId);
        int secondRoom=tx.execute(t->{Room r=new Room();r.setName("Phase alternate room");r.setCapacity(15);r.setStatus("Available");r.setCenterId(1);r.setFacilityType("Indoor");return rooms.save(r).getId();});
        var incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",scheduledId,"startedAt","2026-10-28T00:00:00","reason","Room issue","evidence","Verified maintenance record","requestKey","room-incident"),200);
        var move=new HashMap<String,Object>();move.put("newDate","2026-10-28");move.put("newStartTime","07:45");move.put("newEndTime","09:15");move.put("newRoomId",secondRoom);move.put("cause","CENTER_INCIDENT");move.put("reason","Agreed room replacement");move.put("memberAgreed",true);move.put("requestKey","room-change");move.put("incidentId",incident.get("id").asInt());
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        assertTrue(jdbc.queryForObject("SELECT active FROM session_absence_notices WHERE id=?",Boolean.class,notice.get("id").asInt()));
        proof("CENTER-ROOM-NOTICE","D06,D07","Manager incident with evidence; agreed room replacement at same time","Reschedule 200; notice remains active; no credit change","SELECT active FROM session_absence_notices WHERE id=?",notice.get("id").asInt());
        move.put("cause","MEMBER_EMERGENCY");move.put("newDate","2026-10-29");move.put("newStartTime","06:00");move.put("newEndTime","07:30");move.put("requestKey","emergency-move");move.remove("incidentId");
        call(coach,"POST","/api/sessions/"+scheduledId+"/reschedule",move,403);
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        assertFalse(jdbc.queryForObject("SELECT active FROM session_absence_notices WHERE id=?",Boolean.class,notice.get("id").asInt()));
        assertEquals(11,credit());
        proof("EMERGENCY-MOVE-NOTICE","D05,D07","Old session <12h, new session >=12h; Coach then Manager with consent/reason","Coach 403; Manager/retry 200; actual time change supersedes notice; credit remains 11","SELECT active FROM session_absence_notices WHERE id=?",notice.get("id").asInt());
    }
    Map<String,Object> booking(String key,LocalDate date,String start,String end){
        return new HashMap<>(Map.of("subscriptionId",subscriptionId,"coachId",coachId,"roomId",roomId,"subjectId",subjectId,"sessionDate",date.toString(),"startTime",start,"endTime",end,"agreementConfirmed",true,"requestKey",key));
    }
    int credit(){return jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId);}

    @Test @Order(5) void holidayPreviewRejectsUnresolvedAndStaleChangesAndUnionsIncidentTime() throws Exception {
        LocalDateTime original=deadline();
        var change=Map.of("holidayDate","2026-10-29","holidayName","Phase planned closure","tag","CENTER_EVENT","active",true);
        var blocked=call(manager,"POST","/api/holidays/preview",change,200);assertTrue(blocked.get("blocked").asBoolean());
        call(manager,"POST","/api/holidays",change,409,previewHeader(blocked));assertEquals(original,deadline());
        var incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",scheduledId,"startedAt","2026-10-28T00:00:00","reason","Planned closure affects agreed session","evidence","Verified closure decision","requestKey","holiday-shift"),200);
        int incidentId=incident.get("id").asInt();
        var move=move("holiday-move","2026-10-31","06:00","07:30","CENTER_INCIDENT",incidentId);
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        call(manager,"POST","/api/holidays",change,409,previewHeader(blocked));
        var clear=call(manager,"POST","/api/holidays/preview",change,200);assertFalse(clear.get("blocked").asBoolean());
        int added=call(manager,"POST","/api/holidays",change,201,previewHeader(clear)).get("id").asInt();assertEquals(original.plusDays(1),deadline());
        int october30=jdbc.queryForObject("SELECT holiday_id FROM holidays WHERE holiday_date='2026-10-30'",Integer.class);
        toggleHoliday(october30,false);assertEquals(original,deadline());
        at("2026-10-31T00:00:00");toggleHoliday(added,false);assertEquals(original,deadline());
        assertEquals(LocalDateTime.of(2026,10,30,0,0),jdbc.queryForObject("SELECT actual_closed_until FROM holidays WHERE holiday_id=?",LocalDateTime.class,added));
        toggleHoliday(added,false);assertEquals(original,deadline());
        call(manager,"POST","/api/center-incidents/"+incidentId+"/resolve",Map.of("endedAt","2026-10-31T00:00:00"),200);assertEquals(original.plusDays(2),deadline());
        int overlapping=jdbc.queryForObject("SELECT id FROM center_incidents WHERE request_key='room-incident'",Integer.class);
        call(manager,"POST","/api/center-incidents/"+overlapping+"/resolve",Map.of("endedAt","2026-10-31T00:00:00"),200);
        assertEquals(original.plusDays(2),deadline());assertEquals(11,credit());
        proof("HOLIDAY-IMPACT-UNION","D06,D08,D12,D14","Existing booking conflicts with proposed closure; overlap of 2 incidents and Holiday","Unresolved/stale token 409; clear preview commits; past closure retained; overlapping interruptions add 2 days total, not twice",rightsSql(),subscriptionId);
    }
    @Test @Order(6) void qrAttendanceHonorsExactWindowReplayAndManagerCorrection() throws Exception {
        call(coach,"GET","/api/reports/dashboard",null,403);
        at("2026-10-31T05:59:59");var oldCode=call(member,"GET","/api/attendances/me/checkin-code",null,200);
        assertEquals(1,oldCode.get("expiresInSeconds").asInt());
        call(coach,"POST","/api/attendances/scan",Map.of("memberId",memberId,"sessionId",scheduledId,"code",oldCode.get("payload").asText()),400);
        at("2026-10-31T06:00:00");
        call(coach,"POST","/api/attendances/scan",Map.of("memberId",memberId,"sessionId",scheduledId,"code",oldCode.get("payload").asText()),400);
        var code=call(member,"GET","/api/attendances/me/checkin-code",null,200);assertEquals(30,code.get("expiresInSeconds").asInt());
        var attendance=call(coach,"POST","/api/attendances/scan",Map.of("memberId",memberId,"sessionId",scheduledId,"code",code.get("payload").asText()),200);
        assertEquals("Present",attendance.get("state").asText());int aid=attendance.get("attendanceId").asInt();
        call(coach,"POST","/api/attendances/scan",Map.of("memberId",memberId,"sessionId",scheduledId,"code",code.get("payload").asText()),400);
        at("2026-10-31T07:30:00");var ended=call(member,"GET","/api/attendances/me/checkin-code",null,200);
        call(coach,"POST","/api/attendances/scan",Map.of("memberId",memberId,"sessionId",scheduledId,"code",ended.get("payload").asText()),400);
        finalizer.process(scheduledId);finalizer.process(scheduledId);
        assertTrue(jdbc.queryForObject("SELECT attendance_finalized FROM sessions WHERE session_id=?",Boolean.class,scheduledId));
        call(coach,"POST","/api/attendances/correct",Map.of("attendanceId",aid,"newState","Absent","reason","Correction request"),403);
        call(manager,"POST","/api/attendances/correct",Map.of("attendanceId",aid,"newState","Late","reason","Unsupported new state"),400);
        call(manager,"POST","/api/attendances/correct",Map.of("attendanceId",aid,"newState","Absent","reason","Manager verified recorded result"),200);
        call(manager,"POST","/api/attendances/correct",Map.of("attendanceId",aid,"newState","Present","reason","Manager verified actual attendance"),200);
        assertEquals(11,credit());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=?",Integer.class,scheduledId));
        proof("QR-WINDOW-REPLAY-CORRECTION","D15,D17,D18","Assigned Coach and Member QR; start-1s/start/end; Manager correction after finalizer","TTL 1 then 30; early/old step/replay/end rejected; one Present attendance; only Manager corrects Present/Absent; credit unchanged; Coach report 403","SELECT attendance_id,state FROM attendances WHERE session_id=?",scheduledId);
    }
    @Test @Order(7) void externalRefundAndNarrowCashCorrectionKeepOriginalReceiptsAndMoneyPeriods() throws Exception {
        at("2026-11-01T09:00:00");
        var refund=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",lateInvoiceId,"type","Refund","reason","Verified payment after hold expiry"),200);int rid=refund.get("id").asInt();
        call(cashier,"POST","/api/payment-resolutions/"+rid+"/approve",null,403);
        assertEquals("Approved",call(manager,"POST","/api/payment-resolutions/"+rid+"/approve",null,200).get("status").asText());
        var before=call(manager,"GET","/api/reports/revenue?startDate=2026-11-01&endDate=2026-11-30",null,200);assertEquals(0,before.get("moneyRefunded").decimalValue().signum());
        proof("REFUND-APPROVAL","D10,D13","Late Paid invoice; Receptionist opens, Manager approves external refund","Approved has no refunded money yet","SELECT status,amount,performed_at FROM payment_resolutions WHERE id=?",rid);
        var completion=Map.of("evidence","LOCAL TEST external refund receipt R1","performedAt","2026-11-01T09:00:00","moneyNotReceived",false);
        call(manager,"POST","/api/payment-resolutions/"+rid+"/complete",completion,200);call(manager,"POST","/api/payment-resolutions/"+rid+"/complete",completion,200);
        proof("REFUND-COMPLETE-RETRY","D10,D13","Manager records external evidence with performedAt in November; repeat","Completed once; original invoice Paid/amount unchanged","SELECT status,amount,performed_at FROM payment_resolutions WHERE id=?",rid);
        var bookedOrder=requestOrder("cash-with-history","Cash",member);int historyInvoice=bookedOrder.get("invoiceId").asInt();
        call(cashier,"POST","/api/invoices/"+historyInvoice+"/confirm-cash",Map.of("note","LOCAL TEST actual receipt; booking later cancelled"),200);
        var booking=booking("history-booking",LocalDate.of(2026,11,3),"14:45","16:15");booking.put("subscriptionId",bookedOrder.get("subscriptionId").asInt());
        int bookedId=call(coach,"POST","/api/sessions/book-private",booking,201).get("id").asInt();call(member,"POST","/api/sessions/"+bookedId+"/cancel-private",null,200);
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,bookedOrder.get("subscriptionId").asInt()));
        var protectedCase=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",historyInvoice,"type","CashCorrection","reason","History must block automatic reversal"),200);
        assertEquals("Blocked",call(manager,"POST","/api/payment-resolutions/"+protectedCase.get("id").asInt()+"/approve",null,200).get("status").asText());
        proof("CASH-HISTORY-BLOCK","D16","Cash Advanced has cancelled booking history, credit returned to 12","Correction Blocked despite current 12 credit; original receipt preserved","SELECT status,amount FROM payment_resolutions WHERE id=?",protectedCase.get("id").asInt());
        var wrongOrder=requestOrder("mistaken-cash","Cash",member);int wrongInvoice=wrongOrder.get("invoiceId").asInt(),wrongSub=wrongOrder.get("subscriptionId").asInt();
        call(cashier,"POST","/api/invoices/"+wrongInvoice+"/confirm-cash",Map.of("note","LOCAL TEST mistaken confirmation without receipt"),200);
        var cashCase=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",wrongInvoice,"type","CashCorrection","reason","No money received; counter confirmed by mistake"),200);int cid=cashCase.get("id").asInt();
        assertEquals("Approved",call(manager,"POST","/api/payment-resolutions/"+cid+"/approve",null,200).get("status").asText());
        call(manager,"POST","/api/payment-resolutions/"+cid+"/complete",Map.of("evidence","LOCAL TEST counter reconciliation C1","moneyNotReceived",true),200);
        call(manager,"PATCH","/api/subscriptions/"+wrongSub+"/status",Map.of("status","Active"),400);
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,wrongSub));
        var receipt=call(member,"GET","/api/invoices/"+wrongInvoice+"/receipt",null,200);assertEquals("Paid",receipt.get("paymentStatus").asText());assertTrue(receipt.get("cashConfirmationReversed").asBoolean());assertEquals(3100000,receipt.get("amount").asInt());
        var november=call(manager,"GET","/api/reports/revenue?startDate=2026-11-01&endDate=2026-11-30",null,200);
        assertEquals(3100000,november.get("moneyReceived").asInt());assertEquals(3100000,november.get("moneyRefunded").asInt());assertEquals(0,november.get("netReceived").asInt());assertEquals(3100000,november.get("cashCorrectionsRecorded").asInt());
        var october=call(manager,"GET","/api/reports/revenue?startDate=2026-10-01&endDate=2026-10-31",null,200);assertEquals(12300000,october.get("moneyReceived").asInt());assertEquals(0,october.get("moneyRefunded").asInt());
        proof("CASH-CORRECTION-REPORT","D10,D13,D16","Unused Advanced Cash with no actual money; Manager completes correction","Original Paid 3100000 receipt retained, reversed flag true, subscription Cancelled; Nov received/refunded 3100000 each, net 0; correction 3100000",rightsSql(),wrongSub);
    }
    @Test @Order(8) void incidentWaitingThroughExpiryRestoresThreeRevokedCreditsAndOneCancelledSessionExactlyOnce() throws Exception {
        List<Integer> booked=new ArrayList<>();
        for(int day=2;day<=9;day++)booked.add(call(coach,"POST","/api/sessions/book-private",booking("expiry-book-"+day,LocalDate.of(2026,11,day),"07:45","09:15"),201).get("id").asInt());
        assertEquals(3,credit());
        at("2026-11-08T08:00:00");int affected=booked.get(booked.size()-1);
        var incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",affected,"startedAt","2026-11-08T08:00:00","reason","Verified service interruption","evidence","LOCAL TEST closure report","requestKey","waiting-through-expiry"),200);int id=incident.get("id").asInt();
        assertEquals("WaitingForRecovery",incident.get("status").asText());LocalDateTime prior=deadline();assertEquals(3,credit());
        at(prior.toString());int expired=subscriptionService.expireOutdatedPackages();trace.add("JOB expireOutdatedPackages => "+expired);
        assertEquals(0,credit());assertEquals(3,jdbc.queryForObject("SELECT expiry_revoked_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        call(coach,"POST","/api/sessions/book-private",booking("expired-attempt",prior.toLocalDate().plusDays(2),"06:00","07:30"),400);
        proof("INCIDENT-WAIT-EXPIRY","D08,D12","WaitingForRecovery through exact package deadline; 3 unused credit before expiry","Expired balance 0, expiry_revoked_credit 3; booking rejected",rightsSql(),subscriptionId);
        var resolution=Map.of("endedAt",prior.toString());call(manager,"POST","/api/center-incidents/"+id+"/resolve",resolution,200);assertEquals(3,credit());assertTrue(deadline().isAfter(prior));
        call(manager,"POST","/api/center-incidents/"+id+"/resolve",resolution,200);assertEquals(3,credit());
        var compensation=Map.of("reason","Verified session was not held at all","verifiedNotHeld",true);
        call(manager,"POST","/api/center-incidents/"+id+"/compensate",compensation,200);call(manager,"POST","/api/center-incidents/"+id+"/compensate",compensation,200);assertEquals(4,credit());
        proof("INCIDENT-RESTORE-RETRY","D06,D08","Resolve/retry then compensate/retry verified unheld session","Restore exactly 3 revoked + 1 cancelled credit = 4; never reset 12",rightsSql(),subscriptionId);
        call(manager,"POST","/api/sessions/"+affected+"/reschedule",move("no-history-rewrite",prior.toLocalDate().plusDays(2).toString(),"06:00","07:30","CENTER_INCIDENT",id),400);
        call(coach,"POST","/api/sessions/book-private",booking("rebook-after-recovery",prior.toLocalDate().plusDays(2),"06:00","07:30"),201);assertEquals(3,credit());
        assertEquals(0,jdbc.queryForObject("SELECT expiry_revoked_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        var extra=booking("approved-minimum",LocalDate.of(2027,1,21),"18:15","19:45");
        call(coach,"POST","/api/sessions/book-private",extra,400);
        extra.put("incidentId",id);extra.put("extraValidityReason","Minimum extension for the agreed replacement slot");extra.put("supportReason","Manager supports the agreed makeup");
        call(manager,"POST","/api/sessions/book-private",extra,201);assertEquals(LocalDateTime.of(2027,1,21,19,45),deadline());assertEquals(2,credit());
        proof("INCIDENT-MINIMUM-END","D04,D08,D12","Makeup beyond computed deadline; Coach rejected, Manager reason and incident provided","No historical reschedule; recovered booking allowed; explicit minimum extension ends exactly 2027-01-21T19:45, credit 2",rightsSql(),subscriptionId);
    }
    @Test @Order(9) void normalPaidCourseLateIncidentCreatesOneLinkedMakeupAndCompletesTwelveValidSessions() throws Exception {
        var holiday=Map.of("holidayDate","2027-01-01","holidayName","Phase New Year closure","tag","NATIONAL_FIXED","active",true);
        var preview=call(manager,"POST","/api/holidays/preview",holiday,200);call(manager,"POST","/api/holidays",holiday,201,previewHeader(preview));
        var details=new HashMap<String,Object>();details.put("name","Phase Normal twelve sessions");details.put("subjectId",subjectId);details.put("coachId",coachId);details.put("maxCapacity",15);details.put("classType","Normal");details.put("totalSessions",12);details.put("startDate","2026-12-17");details.put("endDate","2027-02-01");details.put("status","Open");
        var created=call(manager,"POST","/api/classes/normal-with-sessions",Map.of("requestId",UUID.randomUUID().toString(),"sportsClass",details,"roomId",roomId,"daysOfWeek",List.of(2,4),"startTime","07:45","endTime","09:15"),201);int cid=created.get("id").asInt();
        assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=?",Integer.class,cid));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=? AND session_date='2027-01-01'",Integer.class,cid));
        var enrollment=call(member,"POST","/api/classes/"+cid+"/enroll",Map.of("paymentMethod","Cash"),201);int iid=enrollment.get("invoiceId").asInt();
        assertEquals("Pending",enrollment.get("status").asText());call(cashier,"POST","/api/invoices/"+iid+"/confirm-cash",Map.of("note","LOCAL TEST Normal tuition received"),200);
        assertEquals(900000,jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,iid).intValue());
        call(member,"POST","/api/classes/"+cid+"/cancel-enrollment",null,400);
        int original=jdbc.queryForObject("SELECT session_id FROM sessions WHERE class_id=? ORDER BY session_date,start_time LIMIT 1",Integer.class,cid);
        call(member,"POST","/api/sessions/"+original+"/absence",Map.of("reason","Member cannot report Normal absence"),400);
        var cashCase=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",iid,"type","CashCorrection","reason","Normal must not get a cancellation exception"),200);
        assertEquals("Blocked",call(manager,"POST","/api/payment-resolutions/"+cashCase.get("id").asInt()+"/approve",null,200).get("status").asText());
        proof("NORMAL-PAID-NO-CANCEL","D02,D05,D12,D15,D16","12-session Normal course, 900000 Cash; Member personal cancellation/notice","Paid enrollment Registered; 12 sessions skip Holiday; personal cancel/absence 400; Normal CashCorrection Blocked","SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",cid,memberId);
        at("2026-12-17T09:15:00");finalizer.process(original);
        var incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",original,"startedAt","2026-12-17T07:45:00","reason","Late verified center incident; session was not held","evidence","LOCAL TEST incident evidence for original session","requestKey","late-normal-incident"),200);int incidentId=incident.get("id").asInt();
        call(manager,"POST","/api/center-incidents/"+incidentId+"/resolve",Map.of("endedAt","2026-12-17T09:15:00"),200);
        call(manager,"POST","/api/center-incidents/"+incidentId+"/compensate",Map.of("reason","Verified not held at all","verifiedNotHeld",true),200);
        var makeup=Map.of("date","2027-01-28","startTime","07:45","endTime","09:15","roomId",roomId,"coachId",coachId,"reason","Agreed replacement of the unheld Normal session");
        int replacement=call(manager,"POST","/api/center-incidents/"+incidentId+"/normal-makeup",makeup,200).get("id").asInt();
        assertEquals(replacement,call(manager,"POST","/api/center-incidents/"+incidentId+"/normal-makeup",makeup,200).get("id").asInt());
        assertEquals(13,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=?",Integer.class,cid));assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=? AND center_invalidated=false AND status<>'Cancelled'",Integer.class,cid));
        assertEquals(original,jdbc.queryForObject("SELECT makeup_for_session_id FROM sessions WHERE session_id=?",Integer.class,replacement));
        assertEquals("Absent",jdbc.queryForObject("SELECT state FROM attendances WHERE session_id=? AND member_id=?",String.class,original,memberId));
        proof("NORMAL-LATE-INCIDENT-MAKEUP","D06,D08,D15","Verified unheld Normal session already finalized Absent; Manager evidence and makeup retry","13 physical records, 12 valid; exactly one linked makeup; original Absent retained","SELECT session_id,center_invalidated,makeup_for_session_id FROM sessions WHERE class_id=? ORDER BY session_id",cid);
        at("2027-01-23T10:00:00");subscriptionService.expireOutdatedPackages();
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM member_packages WHERE status='Active'",Integer.class));
        var stillEntitled=call(manager,"GET","/api/reports/members",null,200);assertEquals(1,stillEntitled.get("activeMembers").asInt());assertEquals(1,stillEntitled.get("neverEntitledMembers").asInt());
        at("2027-01-28T09:15:00");
        for(Integer sid:jdbc.queryForList("SELECT session_id FROM sessions WHERE class_id=? ORDER BY session_date,start_time",Integer.class,cid))finalizer.process(sid);
        assertEquals("Completed",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,cid,memberId));
        assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=? AND center_invalidated=false AND attendance_finalized=true",Integer.class,cid));
        var completed=call(manager,"GET","/api/reports/members",null,200);assertEquals(0,completed.get("activeMembers").asInt());assertEquals(1,completed.get("previouslyEntitledMembersWithoutRights").asInt());assertEquals(1,completed.get("neverEntitledMembers").asInt());
        proof("NORMAL-COMPLETE-PEOPLE","D13,D15,D18","Only Normal rights remain, then all 12 valid sessions finalize","Active person count 1 before end, then 0; previously entitled 1 and never entitled 1; enrollment Completed","SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",cid,memberId);
    }

    @Test @Order(10) void authenticatedCenterAndActorBoundariesThenCashRace() throws Exception {
        at("2027-02-01T08:00:00");
        tx.executeWithoutResult(t->{
            if(System.getProperty("scms.phases.pg-url","").isBlank())jdbc.update("INSERT INTO centers(center_id) VALUES(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(2,'Isolated acceptance center 2')");
            User c=user("phase.cashier2",roles.findAll().stream().filter(r->"Receptionist".equals(r.getName())).findFirst().orElseThrow());
            c.setCenterId(2);Receptionist profile=new Receptionist();profile.setUser(c);receptionists.save(profile);
            User m=user("phase.manager2",roles.findAll().stream().filter(r->"CenterManager".equals(r.getName())).findFirst().orElseThrow());m.setCenterId(2);
            User extra=user("phase.unassigned",role("Coach","RECORD_RESULT","VIEW_REPORTS"));Coach cp=new Coach();cp.setUser(extra);coaches.save(cp);
        });
        HttpClient cashier2=login("phase.cashier2"),manager2=login("phase.manager2"),unassigned=login("phase.unassigned");
        var order=requestOrder("acceptance-race-cash","Cash",member);invoiceId=order.get("invoiceId").asInt();subscriptionId=order.get("subscriptionId").asInt();
        var payload=booking("center-denied",LocalDate.of(2027,2,2),"06:00","07:30");payload.put("supportReason","Agreed support by manager");
        call(cashier2,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,403);
        call(manager2,"POST","/api/sessions/book-private",payload,403);
        call(cashier,"POST","/api/sessions/book-private",payload,403);
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoiceId));assertEquals(0,credit());
        proof("CROSS-CENTER-ACTOR","D03,D04,D10","Verified sessions: cashier/manager center 2, Receptionist; center 1 Pending order","All three writes 403; original invoice Pending, credit 0; no booking",rightsSql(),subscriptionId);
        call(unassigned,"GET","/api/reports/revenue",null,403);
        var scoped=call(manager2,"GET","/api/reports/revenue",null,200);assertEquals(0,scoped.path("moneyReceived").decimalValue().signum());
        assertEquals(0,scoped.path("totalRevenue").decimalValue().signum());
        proof("REPORT-ROLE-CENTER","D13,D18","Coach has VIEW_REPORTS; Manager center 2 has no receipts while center 1 has Paid history","Coach still 403; Manager 200 scoped moneyReceived/totalRevenue 0","SELECT count(*) AS center_2_invoices FROM invoices i JOIN users u ON u.user_id=i.member_id WHERE u.center_id=2");
        race(new HttpClient[]{cashier,cashier},"/api/invoices/"+invoiceId+"/confirm-cash",new Object[]{null,null},List.of(200,409));
        assertEquals(12,credit());assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoiceId));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE subscription_id=?",Integer.class,subscriptionId));
        proof("CASH-RACE","D03,D10","Two real HTTP Cash confirmations released together by latch, one Pending invoice","Exactly statuses 200/409; single Paid invoice, Active credit 12",rightsSql(),subscriptionId);
        call(unassigned,"POST","/api/sessions/book-private",payload,403);assertEquals(12,credit());
        proof("BOOK-UNASSIGNED-COACH","D04","Active Paid subscription; valid same-center Coach who is not Primary Coach","403; no booking/deduction",rightsSql(),subscriptionId);
    }

    List<JsonNode> race(HttpClient[] clients,String path,Object[] bodies,List<Integer> expectedStatuses) throws Exception {
        int from=exchanges.size();var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
        try {
            var tasks=new ArrayList<Future<JsonNode>>();
            for(int i=0;i<2;i++){final int n=i;tasks.add(pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(clients[n],"POST",path,bodies[n],0);}));}
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            var replies=new ArrayList<JsonNode>();for(var f:tasks)replies.add(f.get(30,TimeUnit.SECONDS));
            var statuses=exchanges.subList(from,exchanges.size()).stream().map(e->(Integer)e.get("actualStatus")).sorted().toList();
            assertEquals(expectedStatuses,statuses,"Concurrent HTTP result set");
            return replies;
        } finally {start.countDown();pool.shutdownNow();}
    }

    @Test @Order(11) void bookingAndCancellationRacesProtectResourcesAndCredit() throws Exception {
        var a=booking("race-book-a",LocalDate.of(2027,2,2),"06:00","07:30");
        var b=new HashMap<>(a);b.put("requestKey","race-book-b");
        race(new HttpClient[]{coach,coach},"/api/sessions/book-private",new Object[]{a,b},List.of(201,409));
        assertEquals(11,credit());
        int id=jdbc.queryForObject("SELECT session_id FROM sessions WHERE subscription_id=? AND status='Scheduled'",Integer.class,subscriptionId);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE subscription_id=?",Integer.class,subscriptionId));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PRIVATE_BOOKING_CONFIRMED:"+id));
        proof("BOOK-RESOURCE-RACE","D04","Two distinct booking keys concurrently target same subscription/Coach/room/time","One 201, one 409; one session and notification; exactly 1 credit consumed",rightsSql(),subscriptionId);
        var winning="race-book-a".equals(jdbc.queryForObject("SELECT booking_key FROM sessions WHERE session_id=?",String.class,id))?a:b;
        var changed=new HashMap<>(winning);changed.put("startTime","07:45");changed.put("endTime","09:15");
        call(coach,"POST","/api/sessions/book-private",changed,409);assertEquals(11,credit());
        call(other,"POST","/api/sessions/"+id+"/cancel-private",null,403);assertEquals(11,credit());
        proof("BOOK-KEY-OWNER","D04,D05","Winning key reused with different slot; Member B targets Member A session","Changed body 409; other owner 403; Scheduled/11 credit unchanged",cancellationSql(),id);
        race(new HttpClient[]{member,member},"/api/sessions/"+id+"/cancel-private",new Object[]{null,null},List.of(200,400));
        assertEquals(12,credit());assertTrue(jdbc.queryForObject("SELECT credit_returned FROM sessions WHERE session_id=?",Boolean.class,id));
        proof("CANCEL-RACE","D05","Two concurrent owner cancellations >=12h before session","One 200, one 400; Cancelled/credit_returned true; credit restored once to 12",cancellationSql(),id);
        var retry=booking("same-key-race",LocalDate.of(2027,2,3),"06:00","07:30");
        var replayed=race(new HttpClient[]{coach,coach},"/api/sessions/book-private",new Object[]{retry,retry},List.of(201,201));
        assertEquals(replayed.get(0).path("id"),replayed.get(1).path("id"));
        assertEquals(11,credit());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE subscription_id=? AND booking_key='same-key-race'",Integer.class,subscriptionId));
        proof("BOOK-SAME-KEY-RACE","D04","Two concurrent identical key/body bookings","Both 201 reference same committed session; one deduction to 11",rightsSql(),subscriptionId);
        var first=booking("different-slot-a",LocalDate.of(2027,2,4),"06:00","07:30");
        var second=booking("different-slot-b",LocalDate.of(2027,2,4),"07:45","09:15");
        race(new HttpClient[]{coach,coach},"/api/sessions/book-private",new Object[]{first,second},List.of(201,201));
        assertEquals(9,credit());
        proof("BOOK-DIFFERENT-SLOT-RACE","D04","Concurrent different slots against same credit balance","Both 201; both deductions retained, balance 9",rightsSql(),subscriptionId);
    }

    @Test @Order(12) void callbackForgeryAndConcurrentValidIpns() throws Exception {
        var order=requestOrder("ipn-negative-race","VNPay",member);int iid=order.path("invoiceId").asInt(),sid=order.path("subscriptionId").asInt();
        String valid=callback(order);
        assertEquals("97",call(member,"GET","/api/payments/vnpay/ipn?"+valid.replaceFirst("vnp_SecureHash=.","vnp_SecureHash=x"),null,200).path("RspCode").asText());
        assertEquals("04",call(member,"GET","/api/payments/vnpay/ipn?"+callback(order,Map.of("vnp_Amount","100")),null,200).path("RspCode").asText());
        assertEquals("01",call(member,"GET","/api/payments/vnpay/ipn?"+callback(order,Map.of("vnp_TxnRef","LOCAL-MISSING")),null,200).path("RspCode").asText());
        assertEquals("97",call(member,"GET","/api/payments/vnpay/ipn?"+callback(order,Map.of("vnp_TmnCode","WRONG")),null,200).path("RspCode").asText());
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,iid));
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
        proof("IPN-INVALID-INPUTS","D03,D10","Local signed callback variants: invalid signature, amount 1 VND, missing reference, wrong merchant","HTTP 200 business RspCodes 97/04/01/97; invoice stays Pending and 0 credit",rightsSql(),sid);
        // GET callback is run concurrently without routing through POST-only race helper.
        int from=exchanges.size();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
        try {
            Callable<JsonNode> action=()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(member,"GET","/api/payments/vnpay/ipn?"+valid,null,200);};
            var a=pool.submit(action);var b=pool.submit(action);assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            assertEquals(List.of("00","02"),java.util.stream.Stream.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)).map(x->x.path("RspCode").asText()).sorted().toList());
        } finally {start.countDown();pool.shutdownNow();}
        assertEquals(2,exchanges.size()-from);assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,iid));
        proof("IPN-RACE","D03,D10","Two identical valid local signed GET IPNs released together","RspCode 00/02; Paid/Active and exactly 12 credit",rightsSql(),sid);
    }

    @Test @Order(13) void exactBookingDeadlineActiveBlockAndReportAuthority() throws Exception {
        at("2027-02-04T07:30:00");
        var order=requestOrder("exact-booking-time","Cash",member);subscriptionId=order.path("subscriptionId").asInt();
        call(cashier,"POST","/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash",null,200);
        assertEquals(LocalDateTime.of(2027,3,21,7,30),deadline());
        var outside=booking("end-after-deadline",LocalDate.of(2027,3,21),"07:45","09:15");
        call(coach,"POST","/api/sessions/book-private",outside,400);assertEquals(12,credit());
        call(coach,"POST","/api/sessions/book-private",booking("end-equal-deadline",LocalDate.of(2027,3,21),"06:00","07:30"),201);assertEquals(11,credit());
        proof("BOOK-EXACT-DEADLINE","D04,D12","Paid at 2027-02-04T07:30; no future holidays; deadline 2027-03-21T07:30","End beyond deadline rejected 400 without deduction; end exactly equal accepted 201, balance 11",rightsSql(),subscriptionId);
        var candidate=booking("member-inactive-blocked",LocalDate.of(2027,2,6),"06:00","07:30");
        tx.executeWithoutResult(t->users.findById(memberId).orElseThrow().setStatus("Inactive"));
        try {call(coach,"POST","/api/sessions/book-private",candidate,400);} finally {tx.executeWithoutResult(t->users.findById(memberId).orElseThrow().setStatus("Active"));}
        tx.executeWithoutResult(t->members.findById(memberId).orElseThrow().setBookingBlockedUntil(LocalDateTime.now(clock).plusDays(7)));
        try {call(coach,"POST","/api/sessions/book-private",candidate,400);} finally {tx.executeWithoutResult(t->members.findById(memberId).orElseThrow().setBookingBlockedUntil(null));}
        assertEquals(11,credit());
        proof("BOOK-MEMBER-ACTIVE-BLOCK","D03,D04,D15","Coach has valid session; Member temporarily Inactive then existing booking block in isolated fixture","Each request 400; no session/deduction; default sanctions toggle not enabled",rightsSql(),subscriptionId);
        tx.executeWithoutResult(t->user("phase.noauthority",role("Admin")));
        var noAuthority=login("phase.noauthority");call(noAuthority,"GET","/api/reports/dashboard",null,403);
        int managerId=users.findByEmailIgnoreCase("phase.manager@example.test").orElseThrow().getId();
        tx.executeWithoutResult(t->users.findById(managerId).orElseThrow().setStatus("Inactive"));
        try {call(manager,"GET","/api/reports/dashboard",null,403);} finally {tx.executeWithoutResult(t->users.findById(managerId).orElseThrow().setStatus("Active"));}
        proof("REPORT-AUTHORITY-ACTIVE","D18","Admin without VIEW_REPORTS; existing Manager session after account becomes Inactive","Both 403; no authority changes to any cloud account","SELECT user_id,status FROM users WHERE user_id=?",managerId);
        at("2027-02-04T18:00:01");var exact=booking("exact-12h-create",LocalDate.of(2027,2,5),"06:00","07:30");
        call(coach,"POST","/api/sessions/book-private",exact,400);assertEquals(11,credit());
        at("2027-02-04T18:00:00");int session=call(coach,"POST","/api/sessions/book-private",exact,201).path("id").asInt();assertEquals(10,credit());
        at("2027-02-04T18:00:01");call(member,"POST","/api/sessions/"+session+"/cancel-private",null,400);assertEquals(10,credit());
        at("2027-02-05T06:00:00");call(member,"POST","/api/sessions/"+session+"/absence",Map.of("reason","Too late at start"),400);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM session_absence_notices WHERE session_id=?",Integer.class,session));
        proof("BOOK-12H-AND-NOTICE-START","D04,D05","Create at notice 12h-1s then exactly 12h; cancel 12h-1s; absence at exact start","Create 400 then 201; late cancel/notice 400; Scheduled with 10 credit, no retrospective notice","SELECT status,credit_returned FROM sessions WHERE session_id=?",session);
    }

    @Test @Order(14) void normalLastSeatRaceHoldsOnlyOneInvoice() throws Exception {
        var eligibility=requestOrder("normal-race-eligibility","Cash",other);
        call(cashier,"POST","/api/invoices/"+eligibility.path("invoiceId").asInt()+"/confirm-cash",null,200);
        var details=new HashMap<String,Object>();details.put("name","Acceptance Normal last seat");details.put("subjectId",subjectId);details.put("coachId",coachId);details.put("maxCapacity",1);details.put("classType","Normal");details.put("totalSessions",12);details.put("startDate","2027-04-01");details.put("endDate","2027-06-01");details.put("status","Open");
        var course=call(manager,"POST","/api/classes/normal-with-sessions",Map.of("requestId",UUID.randomUUID().toString(),"sportsClass",details,"roomId",roomId,"daysOfWeek",List.of(2,4),"startTime","14:45","endTime","16:15"),201);int cid=course.path("id").asInt();
        race(new HttpClient[]{member,other},"/api/classes/"+cid+"/enroll",new Object[]{Map.of("paymentMethod","Cash"),Map.of("paymentMethod","Cash")},List.of(201,201));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Pending'",Integer.class,cid));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Waitlisted'",Integer.class,cid));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,cid));
        int iid=jdbc.queryForObject("SELECT invoice_id FROM invoices WHERE class_id=?",Integer.class,cid);
        call(cashier,"POST","/api/invoices/"+iid+"/confirm-cash",null,200);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Registered'",Integer.class,cid));
        proof("NORMAL-LAST-SEAT-RACE","D10,D15","Two verified Members concurrently enroll in capacity 1 Normal; cashier pays winner","Both 201: one Pending invoice, one Waitlisted without invoice; after Paid exactly one Registered, one Waitlisted","SELECT member_id,status FROM class_enrollments WHERE class_id=? ORDER BY member_id",cid);
    }

    @Test @Order(15) void incidentSubstituteRequiresExactConsentAndKeepsPrimaryCoach() throws Exception {
        int replacement=users.findByEmailIgnoreCase("phase.unassigned@example.test").orElseThrow().getId();
        tx.executeWithoutResult(t->{
            var spec=specs.findAll().stream().filter(s->s.getSubject()!=null&&s.getSubject().getId().equals(subjectId)).findFirst().orElseThrow();
            assignments.save(new CoachSpecialization(coaches.findById(replacement).orElseThrow(),spec));
        });
        var replacementClient=login("phase.unassigned");
        call(manager,"PUT","/api/coaches/"+replacement+"/work-schedule",Map.of("weeklySlots",allSlots(),"daysOff",List.of(),"acceptingNew",true),200);
        int sid=call(coach,"POST","/api/sessions/book-private",booking("substitute-original",LocalDate.of(2027,2,8),"07:45","09:15"),201).path("id").asInt();int before=credit();
        at("2027-02-07T20:00:00");
        int notice=call(member,"POST","/api/sessions/"+sid+"/absence",Map.of("reason","Personal uncertainty before substitution"),200).path("id").asInt();
        int incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",sid,"startedAt","2027-02-07T20:00:00","reason","Primary Coach unavailable","evidence","LOCAL TEST verified staffing incident","requestKey","substitute-incident"),200).path("id").asInt();
        var change=new HashMap<String,Object>(Map.of("newCoachId",replacement,"reason","Member agreed to this Coach for this session","memberAgreed",false,"requestKey","substitute-exact-consent","incidentId",incident));
        assertEquals("AGREEMENT_REQUIRED",call(manager,"POST","/api/sessions/"+sid+"/substitute-coach",change,400).path("code").asText());
        change.put("memberAgreed",true);call(coach,"POST","/api/sessions/"+sid+"/substitute-coach",change,403);
        assertEquals(replacement,call(manager,"POST","/api/sessions/"+sid+"/substitute-coach",change,200).path("coachId").asInt());
        assertEquals(replacement,call(manager,"POST","/api/sessions/"+sid+"/substitute-coach",change,200).path("coachId").asInt());
        change.put("newCoachId",coachId);call(manager,"POST","/api/sessions/"+sid+"/substitute-coach",change,409);
        assertEquals(coachId,jdbc.queryForObject("SELECT primary_coach_id FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        assertTrue(jdbc.queryForObject("SELECT active FROM session_absence_notices WHERE id=?",Boolean.class,notice));assertEquals(before,credit());
        call(replacementClient,"POST","/api/sessions/book-private",booking("substitute-cannot-own-package",LocalDate.of(2027,2,9),"06:00","07:30"),403);
        proof("CENTER-SUBSTITUTE-CONSENT","D04,D06,D07","Qualified same-center substitute Coach with schedule; Manager incident and exact-plan consent; active absence notice","No consent 400, Coach 403; Manager/retry 200; changed plan under same key 409; session Coach changes, Primary Coach/credit/notice preserved; substitute cannot create other package sessions","SELECT s.coach_id,s.status,p.primary_coach_id,p.remaining_credit,n.active AS notice_active FROM sessions s JOIN member_packages p ON p.subscription_id=s.subscription_id JOIN session_absence_notices n ON n.session_id=s.session_id WHERE s.session_id=?",sid);
    }

    Set<String> allSlots(){Set<String> slots=new HashSet<>();for(DayOfWeek d:DayOfWeek.values())for(int n=1;n<=7;n++)slots.add(d.name()+":"+n);return slots;}

    @Test @Order(16) void globalHolidayCannotChangeAnotherCentersEntitlement() throws Exception {
        int[] fixture=tx.execute(t->{
            User m=user("phase.member2",roles.findAll().stream().filter(r->"Member".equals(r.getName())).findFirst().orElseThrow());m.setCenterId(2);Member mp=new Member();mp.setUser(m);mp.setJoinDate(LocalDate.now(clock));members.save(mp);
            User c=user("phase.coach2",roles.findAll().stream().filter(r->"Coach".equals(r.getName())).findFirst().orElseThrow());c.setCenterId(2);Coach cp=new Coach();cp.setUser(c);coaches.save(cp);
            var spec=specs.findAll().stream().filter(s->s.getSubject()!=null&&s.getSubject().getId().equals(subjectId)).findFirst().orElseThrow();assignments.save(new CoachSpecialization(cp,spec));
            Room r=new Room();r.setName("Acceptance center 2 room");r.setCapacity(15);r.setStatus("Available");r.setFacilityType("Indoor");r.setCenterId(2);rooms.save(r);return new int[]{m.getId(),c.getId()};
        });
        var m2=login("phase.member2");var c2=login("phase.coach2");var manager2=login("phase.manager2");var cashier2=login("phase.cashier2");
        call(manager2,"PUT","/api/coaches/"+fixture[1]+"/work-schedule",Map.of("weeklySlots",allSlots(),"daysOff",List.of(),"acceptingNew",true),200);
        var order=requestOrder("center2-holiday-rights","Cash",m2,subjectId,fixture[1],c2);int sub2=order.path("subscriptionId").asInt();
        call(cashier2,"POST","/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash",null,200);
        var before=jdbc.queryForList("SELECT subscription_id,expires_at,remaining_credit FROM member_packages WHERE status='Active' ORDER BY subscription_id");
        var change=Map.of("holidayDate","2027-02-10","holidayName","Global closure needs scope review","tag","CENTER_EVENT","active",true);
        assertEquals("HOLIDAY_SCOPE_REVIEW_REQUIRED",call(manager,"POST","/api/holidays/preview",change,409).path("code").asText());
        assertEquals("HOLIDAY_SCOPE_REVIEW_REQUIRED",call(manager,"POST","/api/holidays",change,409,Map.of("X-Preview-Token","unapproved-global-change")).path("code").asText());
        assertEquals("HOLIDAY_SCOPE_REVIEW_REQUIRED",call(manager2,"POST","/api/holidays/preview",change,409).path("code").asText());
        assertEquals(before,jdbc.queryForList("SELECT subscription_id,expires_at,remaining_credit FROM member_packages WHERE status='Active' ORDER BY subscription_id"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM holidays WHERE holiday_date='2027-02-10'",Integer.class));
        proof("HOLIDAY-OTHER-CENTER","D12,D14","Paid Active rights in centers 1 and 2; Manager from each center proposes global Holiday","Preview/confirm 409 HOLIDAY_SCOPE_REVIEW_REQUIRED; no Holiday inserted; both centers retain exact deadlines and balances","SELECT p.subscription_id,u.center_id,p.expires_at,p.remaining_credit FROM member_packages p JOIN users u ON u.user_id=p.member_id WHERE p.status='Active' ORDER BY p.subscription_id");
    }

    @Test @Order(17) void multiSubjectAdmissionSharesCapacityAndOutdoorHeatBlocksOnlyHotSlots() throws Exception {
        int[] fixture=tx.execute(t->{
            User c=user("phase.multisubject",roles.findAll().stream().filter(r->"Coach".equals(r.getName())).findFirst().orElseThrow());Coach cp=new Coach();cp.setUser(c);coaches.save(cp);
            Subject outdoor=subjects.findBySlug("quan-vot-tennis").orElseGet(Subject::new);outdoor.setSlug("quan-vot-tennis");outdoor.setName("Tennis");outdoor.setStatus("Active");outdoor.setOutdoor(true);outdoor.setNormalPrice(new BigDecimal("2500000"));outdoor.setAdvancedPrice(new BigDecimal("8000000"));subjects.save(outdoor);
            var indoor=specs.findAll().stream().filter(s->s.getSubject()!=null&&s.getSubject().getId().equals(subjectId)).findFirst().orElseThrow();assignments.save(new CoachSpecialization(cp,indoor));
            Specialization tennis=new Specialization();tennis.setName("Acceptance Tennis");tennis.setSubject(outdoor);tennis.setStatus("Active");specs.save(tennis);assignments.save(new CoachSpecialization(cp,tennis));
            Room r=new Room();r.setName("Acceptance outdoor court");r.setCapacity(1);r.setStatus("Available");r.setFacilityType("Outdoor");r.setCenterId(1);rooms.save(r);
            var template=new com.sportscenter.sportclass.SportsClass();template.setName("Acceptance Tennis template");template.setSubject(outdoor);template.setCoach(c);template.setClassType("Advanced");template.setMaxCapacity(1);template.setTuitionFee(outdoor.getAdvancedPrice());template.setStatus("Open");template.setCenterId(1);classes.save(template);
            return new int[]{c.getId(),outdoor.getId(),r.getId(),template.getId()};
        });
        var capacityCoach=login("phase.multisubject");
        call(manager,"PUT","/api/coaches/"+fixture[0]+"/work-schedule",Map.of("weeklySlots",List.of("MONDAY:1","TUESDAY:1"),"daysOff",List.of(),"acceptingNew",true),200);
        var first=requestOrder("shared-capacity-indoor","Cash",member,subjectId,fixture[0],capacityCoach);int firstSub=first.path("subscriptionId").asInt();
        call(cashier,"POST","/api/invoices/"+first.path("invoiceId").asInt()+"/confirm-cash",null,200);
        int prior=jdbc.queryForObject("SELECT count(*) FROM advanced_registrations",Integer.class);
        assertEquals("COACH_CAPACITY_INSUFFICIENT",call(other,"POST","/api/advanced-registrations",Map.of("subjectId",fixture[1],"primaryCoachId",fixture[0],"requestKey","shared-capacity-outdoor"),400).path("code").asText());
        assertEquals(prior,jdbc.queryForObject("SELECT count(*) FROM advanced_registrations",Integer.class));
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,firstSub));
        proof("ADMISSION-MULTISUBJECT","D03","One Coach qualified for Indoor and Outdoor; 2 weekly slots over 45 days cover one 12-credit package, not two","First Paid package Active/12; second subject registration 400 COACH_CAPACITY_INSUFFICIENT, no registration/Invoice or new credit",rightsSql(),firstSub);
        call(manager,"PUT","/api/coaches/"+fixture[0]+"/work-schedule",Map.of("weeklySlots",allSlots(),"daysOff",List.of(),"acceptingNew",true),200);
        var tennis=requestOrder("outdoor-after-capacity-expanded","Cash",member,fixture[1],fixture[0],capacityCoach);int tennisSub=tennis.path("subscriptionId").asInt();
        call(cashier,"POST","/api/invoices/"+tennis.path("invoiceId").asInt()+"/confirm-cash",null,200);
        var payload=booking("outdoor-hot-slot",LocalDate.of(2027,2,12),"09:30","11:00");payload.put("subscriptionId",tennisSub);payload.put("coachId",fixture[0]);payload.put("subjectId",fixture[1]);payload.put("roomId",fixture[2]);payload.put("classId",fixture[3]);
        assertEquals("OUTDOOR_HEAT_BLOCK",call(capacityCoach,"POST","/api/sessions/book-private",payload,400).path("code").asText());
        payload.put("startTime","13:00");payload.put("endTime","14:30");
        assertEquals("OUTDOOR_HEAT_BLOCK",call(capacityCoach,"POST","/api/sessions/book-private",payload,400).path("code").asText());
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,tennisSub));
        payload.put("startTime","06:00");payload.put("endTime","07:30");call(capacityCoach,"POST","/api/sessions/book-private",payload,201);
        assertEquals(11,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,tennisSub));
        proof("OUTDOOR-HEAT-HTTP","D04,D12","Paid Tennis/Outdoor metadata; Coach published all 7 slots; dedicated Outdoor court","Slots 3/4 each 400 OUTDOOR_HEAT_BLOCK without deduction; slot 1 accepted 201 and consumes only 1 credit",rightsSql(),tennisSub);
    }

    String profileSql(){return "SELECT u.user_id,u.full_name,u.email,u.phone,u.address,u.date_of_birth,u.gender,u.status,u.center_id,m.goal,m.health_note FROM users u JOIN members m ON m.user_id=u.user_id WHERE u.user_id=?";}
    @Test @Order(18) void selfProfileUsesOnlyOwnerFieldsAndProtectsConcurrentPatches() throws Exception {
        var initial=call(member,"GET","/api/members/me",null,200);assertEquals("phase.member",initial.path("name").asText());assertEquals(7,initial.size());
        call(coach,"GET","/api/members/me",null,403);
        var forged=new HashMap<String,Object>();forged.put("name","Profile updated");forged.put("phone","0901234567");forged.put("goal","Run");forged.put("memberId",otherId);forged.put("role","Admin");forged.put("centerId",2);forged.put("status","Inactive");forged.put("credit",999);forged.put("amount",1);
        var result=call(member,"PATCH","/api/members/me",forged,200);assertEquals(7,result.size());assertEquals("Profile updated",result.path("name").asText());
        assertEquals("phase.other",jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,otherId));
        assertEquals("Active",jdbc.queryForObject("SELECT status FROM users WHERE user_id=?",String.class,memberId));assertEquals(1,jdbc.queryForObject("SELECT center_id FROM users WHERE user_id=?",Integer.class,memberId));
        assertEquals("Member",jdbc.queryForObject("SELECT r.role_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.user_id=?",String.class,memberId));
        call(member,"GET","/api/members/"+otherId,null,403);
        proof("PROFILE-OWNER-WHITELIST","D01","Verified Member A session; forged Member B/role/center/status/credit/amount in PATCH; Coach valid session","Only supplied editable fields change; exactly 7 response fields; owner remains Active/Member/center1 and Member B unchanged; Coach and other-owner admin route 403",profileSql(),memberId);
        var clear=new HashMap<String,Object>();clear.put("phone",null);clear.put("address",null);clear.put("goal",null);clear.put("healthNote",null);
        call(member,"PATCH","/api/members/me",clear,200);call(member,"PATCH","/api/members/me",Map.of(),200);
        assertNull(jdbc.queryForObject("SELECT phone FROM users WHERE user_id=?",String.class,memberId));assertNull(jdbc.queryForObject("SELECT goal FROM members WHERE user_id=?",String.class,memberId));
        call(member,"PATCH","/api/members/me",Map.of("goal","Temporary","dateOfBirth",LocalDate.now(clock).plusDays(1).toString()),400);
        assertNull(jdbc.queryForObject("SELECT goal FROM members WHERE user_id=?",String.class,memberId));
        assertEquals("FUTURE_DATE_OF_BIRTH",call(member,"PATCH","/api/members/me",Map.of("dateOfBirth",LocalDate.now(clock).plusDays(1).toString()),400).path("code").asText());
        call(member,"PATCH","/api/members/me",Map.of("dateOfBirth",LocalDate.now(clock).toString()),200);
        proof("PROFILE-NULL-DATE-ATOMIC","D01","Nullable fields cleared; absent keys retained; empty PATCH; date of birth today/tomorrow using business Clock","Cleared values persist; no-op leaves name; invalid future date 400 rolls back supplied goal; today accepted",profileSql(),memberId);
        profilePatchRace();
        assertEquals("Race phone",jdbc.queryForObject("SELECT phone FROM users WHERE user_id=?",String.class,memberId));
        assertEquals("Race address",jdbc.queryForObject("SELECT address FROM users WHERE user_id=?",String.class,memberId));
        proof("PROFILE-PATCH-RACE","D01","Two concurrent PATCH requests on distinct fields of same owner; PostgreSQL row held before releasing both waiting requests","Both 200; phone and address updates both survive commit; no lost update",profileSql(),memberId);
        tx.executeWithoutResult(t->users.findById(otherId).orElseThrow().setFirstLogin(true));
        assertEquals("PASSWORD_CHANGE_REQUIRED",call(other,"GET","/api/members/me",null,403).path("code").asText());
        call(other,"POST","/api/auth/change-password",Map.of("oldPassword","LocalTest123!","newPassword","ChangedLocal123!"),204);
        call(other,"GET","/api/auth/me",null,200);call(other,"GET","/api/members/me",null,200);
        proof("PROFILE-FIRST-LOGIN","D01","Member B already authenticated; firstLogin flag set; changes password through existing allowed route","Profile 403 PASSWORD_CHANGE_REQUIRED before change; password route 204 then profile 200; global filter unchanged",profileSql(),otherId);
        tx.executeWithoutResult(t->users.findById(memberId).orElseThrow().setStatus("Inactive"));
        try {call(member,"GET","/api/members/me",null,403);call(member,"PATCH","/api/members/me",Map.of("name","Must not persist"),403);
            assertEquals("Profile updated",jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,memberId));
            proof("PROFILE-INACTIVE-SESSION","D01","Previously valid Member session after account is disabled in local fixture","Both GET/PATCH 403; account stays Inactive and profile unchanged; old session cannot bypass disable",profileSql(),memberId);}
        finally {tx.executeWithoutResult(t->users.findById(memberId).orElseThrow().setStatus("Active"));}
    }
    void profilePatchRace() throws Exception {
        boolean pg=!System.getProperty("scms.phases.pg-url","").isBlank();
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try(var blocker=Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            if(pg){blocker.setAutoCommit(false);try(var s=blocker.prepareStatement("SELECT user_id FROM users WHERE user_id=? FOR UPDATE")){s.setInt(1,memberId);s.executeQuery().close();}}
            var a=pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(member,"PATCH","/api/members/me",Map.of("phone","Race phone"),200);});
            var b=pool.submit(()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return call(member,"PATCH","/api/members/me",Map.of("address","Race address"),200);});
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            if(pg){long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);int waiting=0;while(System.nanoTime()<until){waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'",Integer.class);if(waiting>=2)break;Thread.sleep(20);}blocker.commit();assertTrue(waiting>=2,"Both requests must wait on the controlled PostgreSQL lock");}
            a.get(30,TimeUnit.SECONDS);b.get(30,TimeUnit.SECONDS);
        } finally {start.countDown();pool.shutdownNow();}
    }

    @Test @Order(19) void directoryRealSessionsRespectScopeContactsAndConstantQueries() throws Exception {
        int[] ids=tx.execute(t->{
            if(jdbc.queryForObject("SELECT count(*) FROM centers WHERE center_id=2",Integer.class)==0){if(System.getProperty("scms.phases.pg-url","").isBlank())jdbc.update("INSERT INTO centers(center_id) VALUES(2)");else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(2,'Directory center 2')");}
            var coachRole=roles.findAll().stream().filter(r->"Coach".equals(r.getName())).findFirst().orElseThrow();
            User empty=user("phase.directoryempty",coachRole);Coach ec=new Coach();ec.setUser(empty);coaches.save(ec);
            User outside=user("phase.directoryoutside",coachRole);outside.setCenterId(2);Coach oc=new Coach();oc.setUser(outside);coaches.save(oc);
            Specialization extra=new Specialization();extra.setName("Directory extra");extra.setStatus("Active");extra.setSubject(subjects.findById(subjectId).orElseThrow());specs.save(extra);assignments.save(new CoachSpecialization(coaches.findById(coachId).orElseThrow(),extra));
            User admin=user("phase.directoryadmin",role("Admin","MANAGE_USERS"));admin.setCenterId(null);
            User contact=user("phase.directorycontact",role("CenterManager","MANAGE_USERS"));
            if(System.getProperty("scms.phases.pg-url","").isBlank()){User missing=user("phase.directorymissing",roles.findAll().stream().filter(r->"Member".equals(r.getName())).findFirst().orElseThrow());missing.setCenterId(null);Member mp=new Member();mp.setUser(missing);mp.setJoinDate(LocalDate.now(clock));members.save(mp);}
            return new int[]{empty.getId(),outside.getId(),extra.getId(),admin.getId(),contact.getId()};
        });
        var admin=login("phase.directoryadmin");var contact=login("phase.directorycontact");
        var scoped=call(member,"GET","/api/coaches",null,200);
        assertTrue(java.util.stream.StreamSupport.stream(scoped.spliterator(),false).noneMatch(c->c.path("id").asInt()==ids[1]));
        assertTrue(java.util.stream.StreamSupport.stream(scoped.spliterator(),false).filter(c->c.path("id").asInt()==ids[0]).findFirst().orElseThrow().path("specializations").isEmpty());
        for(var c:scoped)assertTrue(c.path("email").isNull());
        var filtered=call(member,"GET","/api/coaches?specializationId="+ids[2]+"&keyword=%20DiReCtOrY%20extra%20",null,200);assertEquals(1,filtered.size());assertEquals(coachId,filtered.get(0).path("id").asInt());assertEquals(2,filtered.get(0).path("specializations").size());
        assertEquals(0,call(member,"GET","/api/coaches?keyword=missing-directory-name",null,200).size());
        var global=call(admin,"GET","/api/coaches",null,200);assertTrue(java.util.stream.StreamSupport.stream(global.spliterator(),false).anyMatch(c->c.path("id").asInt()==ids[1]));
        for(var c:call(contact,"GET","/api/coaches",null,200))assertFalse(c.path("email").isNull());
        if(System.getProperty("scms.phases.pg-url","").isBlank())call(login("phase.directorymissing"),"GET","/api/coaches",null,400);
        proof("DIRECTORY-SCOPE-CONTACT","D09","Actual HTTP sessions: Member, Manager+MANAGE_USERS, Admin with null center; empty Coach and two specializations; invalid missing-center Member exists only in H2 (PostgreSQL guard rejects fixture)","Member sees only own center with contact null; combined filter keeps complete specialization set; empty Coach []; unmatched filter []; Admin sees both centers on PostgreSQL; MANAGE_USERS reveals contact; missing-center 400 is H2-only","SELECT u.user_id,u.center_id FROM coaches c JOIN users u ON u.user_id=c.user_id ORDER BY u.user_id");
        var statistics=Objects.requireNonNull(entityManagerFactory).unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();int before=call(member,"GET","/api/coaches",null,200).size();long queriesBefore=statistics.getPrepareStatementCount();
        tx.executeWithoutResult(t->{for(int n=0;n<6;n++){User u=user("phase.directoryextra"+n,roles.findAll().stream().filter(r->"Coach".equals(r.getName())).findFirst().orElseThrow());Coach c=new Coach();c.setUser(u);coaches.save(c);}});
        statistics.clear();assertEquals(before+6,call(member,"GET","/api/coaches",null,200).size());long queriesAfter=statistics.getPrepareStatementCount();assertEquals(queriesBefore,queriesAfter);
        proof("DIRECTORY-NPLUS1","D09","Six additional same-center Coach profiles after first complete HTTP directory request; OSIV=false","No LazyInitializationException; prepared statement count stays constant as Coach count grows","SELECT count(*) AS coaches_in_center_1 FROM coaches c JOIN users u ON u.user_id=c.user_id WHERE u.center_id=1");
        acceptance.get(acceptance.size()-1).put("statementCounts",Map.of("before",queriesBefore,"after",queriesAfter));
        tx.executeWithoutResult(t->users.findById(ids[4]).orElseThrow().setStatus("Inactive"));
        try {call(contact,"GET","/api/coaches",null,403);
            proof("DIRECTORY-INACTIVE-SESSION","D09","Manager with MANAGE_USERS has an old session after local account is disabled","Directory 403, account stays Inactive; no contact disclosure through inactive session","SELECT user_id,status FROM users WHERE user_id=?",ids[4]);}
        finally {tx.executeWithoutResult(t->users.findById(ids[4]).orElseThrow().setStatus("Active"));}
    }

    @Test @Order(99) void publishedOpenApiKeepsDistinctWorkflowDtosAndActualSuccessStatuses() throws Exception {
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v3/api-docs")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());JsonNode spec=json.readTree(response.body());
        String evidenceDir=System.getProperty("scms.evidence-dir","");if(!evidenceDir.isBlank()){Files.createDirectories(Path.of(evidenceDir));((com.fasterxml.jackson.databind.node.ObjectNode)spec).putArray("servers").addObject().put("url","https://scms-app-p1gy.onrender.com").put("description","Canonical FE server; schema generated locally");json.writerWithDefaultPrettyPrinter().writeValue(Path.of(evidenceDir).resolve("openapi.json").toFile(),spec);}
        Map<String,List<String>> fields=Map.of(
                "AdvancedRegistrationRequest",List.of("subjectId","primaryCoachId","requestKey"),
                "NormalMakeupRequest",List.of("date","startTime","endTime","roomId","coachId","reason"),
                "CenterIncidentOpenRequest",List.of("sessionId","startedAt","reason","evidence","requestKey"),
                "PaymentResolutionOpenRequest",List.of("invoiceId","type","reason"));
        var profileFields=Set.of("name","phone","address","dateOfBirth","gender","goal","healthNote");
        for(String schema:List.of("MemberSelfProfilePatch","MemberSelfProfileResponse")){Set<String> actual=new HashSet<>();spec.path("components").path("schemas").path(schema).path("properties").fieldNames().forEachRemaining(actual::add);assertEquals(profileFields,actual,schema);}
        assertSchemaRef(spec,"/api/members/me","patch",true,"MemberSelfProfilePatch");
        for(var entry:fields.entrySet()) {
            Set<String> actual=new HashSet<>();spec.path("components").path("schemas").path(entry.getKey()).path("properties").fieldNames().forEachRemaining(actual::add);
            assertEquals(new HashSet<>(entry.getValue()),actual,entry.getKey());
        }
        assertSchemaRef(spec,"/api/advanced-registrations","post",true,"AdvancedRegistrationRequest");
        assertSchemaRef(spec,"/api/center-incidents","post",true,"CenterIncidentOpenRequest");
        assertSchemaRef(spec,"/api/payment-resolutions","post",true,"PaymentResolutionOpenRequest");
        assertSchemaRef(spec,"/api/center-incidents/{id}/normal-makeup","post",true,"NormalMakeupRequest");
        assertSchemaRef(spec,"/api/center-incidents","post",false,"CenterIncidentResponse");
        assertSchemaRef(spec,"/api/payment-resolutions","post",false,"PaymentResolutionResponse");
        var centerProperties=spec.path("components").path("schemas").path("CenterIncidentResponse").path("properties");
        assertTrue(centerProperties.has("sessionId"));assertTrue(centerProperties.has("recoveredCredits"));assertFalse(centerProperties.has("invoiceId"));
        var moneyProperties=spec.path("components").path("schemas").path("PaymentResolutionResponse").path("properties");
        assertTrue(moneyProperties.has("invoiceId"));assertTrue(moneyProperties.has("amount"));assertFalse(moneyProperties.has("sessionId"));
        assertEquals("string",spec.path("components").path("schemas").path("LocalTime").path("type").asText());
        List<String> created=List.of("/api/classes","/api/classes/normal-with-sessions","/api/classes/{classId}/enroll","/api/enrollments",
                "/api/classes/{classId}/feedback","/api/holidays","/api/packages","/api/rooms","/api/sessions","/api/sessions/book-private",
                "/api/sessions/generate","/api/subjects","/api/training-plans","/api/evaluations");
        for(String path:created)assertTrue(spec.path("paths").path(path).path("post").path("responses").has("201"),path);
        List<String> noContentPost=List.of("/api/auth/change-password","/api/auth/logout","/api/auth/reset-password","/api/auth/verify-registration","/api/chat/conversations/{id}/read");
        for(String path:noContentPost)assertTrue(spec.path("paths").path(path).path("post").path("responses").has("204"),path);
        List<String> noContentDelete=List.of("/api/evaluations/{id}","/api/sessions/{id}","/api/subjects/{id}","/api/training-plans/{id}","/api/users/me/avatar");
        for(String path:noContentDelete)assertTrue(spec.path("paths").path(path).path("delete").path("responses").has("204"),path);
        openApiProof.put("result","PASS");openApiProof.put("distinctRequestFields",fields);openApiProof.put("responseSchemas",List.of("CenterIncidentResponse","PaymentResolutionResponse"));
        openApiProof.put("created201",created);openApiProof.put("post204",noContentPost);openApiProof.put("delete204",noContentDelete);
        int checked=0;
        for(var exchange:exchanges) {
            int status=(int)exchange.get("actualStatus");if(status<200||status>=300)continue;
            String path=exchange.get("path").toString().split("\\?")[0];String method=exchange.get("method").toString().toLowerCase(Locale.ROOT);
            JsonNode operation=null;String matchedPath=null;
            for(var it=spec.path("paths").fields();it.hasNext();) {
                var entry=it.next();
                if(path.matches(entry.getKey().replaceAll("\\{[^}]+}","[^/]+"))&&entry.getValue().has(method)
                        &&(matchedPath==null||entry.getKey().chars().filter(c->c=='{').count()<matchedPath.chars().filter(c->c=='{').count())){operation=entry.getValue().get(method);matchedPath=entry.getKey();}
            }
            assertNotNull(operation,path);JsonNode result=operation.path("responses").path(String.valueOf(status));assertFalse(result.isMissingNode(),path+" status "+status);
            var input=operation.path("requestBody").path("content").elements();
            if(input.hasNext())assertWireShape((JsonNode)exchange.get("requestBody"),input.next().path("schema"),spec,"request "+path);
            var output=result.path("content").elements();
            if(output.hasNext()&&exchange.get("responseBody") instanceof JsonNode value)assertWireShape(value,output.next().path("schema"),spec,"response "+path);
            checked++;
        }
        openApiProof.put("positiveExchangesCheckedForStatusTypeAndProperties",checked);openApiProof.put("localTimeWireType","string");
        openApiProof.put("source","Real HTTP GET /v3/api-docs in the same local application as the workflows");
        trace.add("GET /v3/api-docs => 200 [distinct workflow DTOs and 24 success status assertions passed; full spec omitted]");
    }
    void assertSchemaRef(JsonNode spec,String path,String method,boolean request,String name) {
        JsonNode operation=spec.path("paths").path(path).path(method);
        JsonNode content=request?operation.path("requestBody").path("content"):operation.path("responses").path("200").path("content");
        assertTrue(content.elements().hasNext(),path);JsonNode schema=content.elements().next().path("schema");
        assertEquals("#/components/schemas/"+name,schema.path("$ref").asText(),path);
    }
    void assertWireShape(JsonNode value,JsonNode schema,JsonNode spec,String where) {
        if(value==null||value.isNull())return; // Legacy nullable values are documented separately; no false non-null guarantee.
        if(schema.has("$ref")){assertWireShape(value,spec.at(schema.get("$ref").asText().substring(1)),spec,where);return;}
        switch(schema.path("type").asText()) {
            case "object" -> {
                assertTrue(value.isObject(),where+" expected object");var props=schema.path("properties");
                value.fields().forEachRemaining(entry->{
                    if(props.has(entry.getKey()))assertWireShape(entry.getValue(),props.get(entry.getKey()),spec,where+"."+entry.getKey());
                    else assertTrue(schema.has("additionalProperties"),where+" undeclared property "+entry.getKey());
                });
            }
            case "array" -> {assertTrue(value.isArray(),where+" expected array");for(var item:value)assertWireShape(item,schema.path("items"),spec,where+"[]");}
            case "string" -> assertTrue(value.isTextual(),where+" expected string");
            case "integer" -> assertTrue(value.isIntegralNumber(),where+" expected integer");
            case "number" -> assertTrue(value.isNumber(),where+" expected number");
            case "boolean" -> assertTrue(value.isBoolean(),where+" expected boolean");
            default -> { }
        }
    }

    Map<String,String> previewHeader(JsonNode preview){return Map.of("X-Preview-Token",preview.get("previewToken").asText());}
    void toggleHoliday(int id,boolean active) throws Exception{var p=call(manager,"POST","/api/holidays/preview",Map.of("holidayId",id,"active",active),200);call(manager,"PATCH","/api/holidays/"+id+"/toggle",Map.of("active",active),200,previewHeader(p));}
    Map<String,Object> move(String key,String date,String start,String end,String cause,Integer incidentId){var p=new HashMap<String,Object>();p.put("newDate",date);p.put("newStartTime",start);p.put("newEndTime",end);p.put("reason","Agreed and verified schedule change");p.put("cause",cause);p.put("memberAgreed",true);p.put("requestKey",key);if(incidentId!=null)p.put("incidentId",incidentId);return p;}
    void at(String local){clock.current=LocalDateTime.parse(local).atZone(clock.getZone()).toInstant();}
    LocalDateTime deadline(){return jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",LocalDateTime.class,subscriptionId);}
    String callback(JsonNode order) throws Exception {
        return callback(order,Map.of());
    }
    String callback(JsonNode order,Map<String,String> overrides) throws Exception {
        TreeMap<String,String> p=new TreeMap<>();p.put("vnp_TmnCode","LOCALTEST");p.put("vnp_TxnRef",order.get("invoiceCode").asText());p.put("vnp_Amount",order.get("amount").decimalValue().movePointRight(2).toPlainString().split("\\.")[0]);p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");p.put("vnp_TransactionNo","LOCAL-"+order.get("invoiceId").asText());
        p.putAll(overrides);
        String q=p.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec("phase-workflow-test-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return q+"&vnp_SecureHash="+HexFormat.of().formatHex(mac.doFinal(q.getBytes(StandardCharsets.UTF_8)));
    }
    @AfterAll void evidence() throws Exception {
        String dir=System.getProperty("scms.evidence-dir","");
        if(!dir.isBlank()) {Path p=Path.of(dir);Files.createDirectories(p);Files.write(p.resolve("http-workflow.txt"),trace,StandardCharsets.UTF_8);
            json.writerWithDefaultPrettyPrinter().writeValue(p.resolve("http-exchanges.json").toFile(),exchanges);}
        if(!dir.isBlank())json.writerWithDefaultPrettyPrinter().writeValue(Path.of(dir).resolve("openapi-contract-check.json").toFile(),openApiProof);
        if(!dir.isBlank())json.writerWithDefaultPrettyPrinter().writeValue(Path.of(dir).resolve("acceptance-cases.json").toFile(),acceptance);
        if(!dir.isBlank())json.writerWithDefaultPrettyPrinter().writeValue(Path.of(dir).resolve("actors.json").toFile(),
                jdbc.queryForList("SELECT user_id,email,full_name,center_id,status FROM users ORDER BY user_id"));
    }
}
