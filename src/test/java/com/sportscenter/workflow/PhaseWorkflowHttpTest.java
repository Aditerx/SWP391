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
    @Autowired MutableClock clock;
    @LocalServerPort int port;
    int memberId,coachId,otherId,subjectId,roomId,invoiceId,subscriptionId,lateInvoiceId;
    HttpClient member,coach,manager,cashier,other;
    String invoiceCode;
    final List<String> trace=new ArrayList<>();
    final List<Map<String,Object>> exchanges=new ArrayList<>();
    String currentScenario="fixture";

    @BeforeEach void scenario(TestInfo info) {
        currentScenario=info.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
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
    HttpClient login(String prefix) throws Exception {HttpClient c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();call(c,"POST","/api/auth/login",Map.of("email",prefix+"@example.test","password","LocalTest123!"),200);return c;}
    JsonNode call(HttpClient client,String method,String path,Object body,int expected) throws Exception {
        return call(client,method,path,body,expected,Map.of());
    }
    JsonNode call(HttpClient client,String method,String path,Object body,int expected,Map<String,String> headers) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
        headers.forEach(b::header);
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
        if(!path.equals("/api/auth/login"))trace.add((method+" "+path.replaceAll("vnp_SecureHash=[^&]*","vnp_SecureHash=[redacted]")+" => "+response.statusCode()+" "+(path.contains("checkin-code")?"[QR payload redacted]":response.body().replaceAll("vnp_SecureHash=[^\"&]*","vnp_SecureHash=[redacted]"))).stripTrailing());
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
            item.put("responseBody",responseBody);exchanges.add(item);
        }
        assertEquals(expected,response.statusCode(),method+" "+path+": "+response.body());
        return response.body().isBlank()||response.statusCode()==302?json.createObjectNode():json.readTree(response.body());
    }
    String actor(HttpClient client) {
        if(client==member)return "MEMBER_A";if(client==other)return "MEMBER_B";
        if(client==coach)return "PRIMARY_COACH";if(client==manager)return "CENTER_MANAGER";
        if(client==cashier)return "RECEPTIONIST";return "FIXTURE_LOGIN";
    }
    JsonNode safeRequest(Object body) {
        JsonNode result=json.valueToTree(body);
        if(result.isObject())for(String key:List.of("code","password","otp","payload"))
            if(result.has(key))((com.fasterxml.jackson.databind.node.ObjectNode)result).put(key,"[redacted]");
        return result;
    }
    JsonNode requestOrder(String key,String method,HttpClient owner) throws Exception {
        var r=call(owner,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey",key),200);
        int id=r.get("id").asInt();
        assertEquals(id,call(owner,"POST","/api/advanced-registrations",Map.of("subjectId",subjectId,"primaryCoachId",coachId,"requestKey",key),200).get("id").asInt());
        call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),400);
        call(coach,"POST","/api/advanced-registrations/"+id+"/decision",Map.of("accepted",true),200);
        var order=call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),200);
        assertEquals(order.get("invoiceId"),call(owner,"POST","/api/advanced-registrations/"+id+"/order",Map.of("paymentMethod",method),200).get("invoiceId"));
        return order;
    }
    @Test @Order(1) void acceptanceCashPriceSnapshotAndIdempotency() throws Exception {
        var order=requestOrder("cash-one","Cash",member);invoiceId=order.get("invoiceId").asInt();subscriptionId=order.get("subscriptionId").asInt();invoiceCode=order.get("invoiceCode").asText();
        assertEquals(3000000,order.get("amount").asInt());
        assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        tx.executeWithoutResult(t->{var s=subjects.findById(subjectId).orElseThrow();s.setAdvancedPrice(new BigDecimal("3100000"));});
        call(other,"GET","/api/members/me/invoices/"+invoiceId,null,404);
        call(manager,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,403);
        call(cashier,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,200);
        call(cashier,"POST","/api/invoices/"+invoiceId+"/confirm-cash",null,409);
        assertEquals(new BigDecimal("3000000.00"),jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoiceId));
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        assertEquals(LocalDateTime.of(2026,12,12,8,0),jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",LocalDateTime.class,subscriptionId));
    }
    @Test @Order(2) void returnCannotPayAndSignedIpnReplaysOnlyOnce() throws Exception {
        var order=requestOrder("online-one","VNPay",member);int iid=order.get("invoiceId").asInt();int sid=order.get("subscriptionId").asInt();
        String query=callback(order);
        call(member,"GET","/api/payments/vnpay/return?"+query,null,302);
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,iid));
        assertEquals("00",call(member,"GET","/api/payments/vnpay/ipn?"+query,null,200).get("RspCode").asText());
        assertEquals("02",call(member,"GET","/api/payments/vnpay/ipn?"+query,null,200).get("RspCode").asText());
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,sid));
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
        // A verified, on-time receipt must survive a business failure without granting unusable rights.
        var unavailable=requestOrder("on-time-unavailable","VNPay",member);
        jdbc.update("UPDATE subjects SET status='Inactive' WHERE subject_id=?",subjectId);
        try {
            assertEquals("00",call(member,"GET","/api/payments/vnpay/ipn?"+callback(unavailable),null,200).get("RspCode").asText());
            assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,unavailable.get("invoiceId").asInt()));
            assertTrue(jdbc.queryForObject("SELECT reconciliation_required FROM invoices WHERE invoice_id=?",Boolean.class,unavailable.get("invoiceId").asInt()));
            assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,unavailable.get("subscriptionId").asInt()));
        } finally { jdbc.update("UPDATE subjects SET status='Active' WHERE subject_id=?",subjectId); }
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
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PRIVATE_BOOKING_CONFIRMED:"+id));
        call(manager,"PATCH","/api/sessions/"+id+"/status",Map.of("status","Completed"),400);
        call(manager,"PATCH","/api/subscriptions/"+subscriptionId+"/status",Map.of("status","Expired"),400);
        assertEquals("Scheduled",jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,id));assertEquals(11,credit());
        clock.current=LocalDateTime.of(2026,10,27,18,0).atZone(clock.getZone()).toInstant();
        call(member,"POST","/api/sessions/"+id+"/cancel-private",null,200);
        call(member,"POST","/api/sessions/"+id+"/cancel-private",null,400);
        assertEquals(12,credit());
        var next=call(coach,"POST","/api/sessions/book-private",booking("notice-session",LocalDate.of(2026,10,28),"07:45","09:15"),201);
        scheduledId=next.get("id").asInt();
        clock.current=LocalDateTime.of(2026,10,28,0,0).atZone(clock.getZone()).toInstant();
        var notice=call(member,"POST","/api/sessions/"+scheduledId+"/absence",Map.of("reason","Unexpected personal issue"),200);
        assertTrue(notice.get("active").asBoolean());assertEquals(11,credit());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM attendances WHERE session_id=?",Integer.class,scheduledId));
        call(member,"POST","/api/sessions/"+scheduledId+"/cancel-private",null,400);
        int secondRoom=tx.execute(t->{Room r=new Room();r.setName("Phase alternate room");r.setCapacity(15);r.setStatus("Available");r.setCenterId(1);r.setFacilityType("Indoor");return rooms.save(r).getId();});
        var incident=call(manager,"POST","/api/center-incidents",Map.of("sessionId",scheduledId,"startedAt","2026-10-28T00:00:00","reason","Room issue","evidence","Verified maintenance record","requestKey","room-incident"),200);
        var move=new HashMap<String,Object>();move.put("newDate","2026-10-28");move.put("newStartTime","07:45");move.put("newEndTime","09:15");move.put("newRoomId",secondRoom);move.put("cause","CENTER_INCIDENT");move.put("reason","Agreed room replacement");move.put("memberAgreed",true);move.put("requestKey","room-change");move.put("incidentId",incident.get("id").asInt());
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        assertTrue(jdbc.queryForObject("SELECT active FROM session_absence_notices WHERE id=?",Boolean.class,notice.get("id").asInt()));
        move.put("cause","MEMBER_EMERGENCY");move.put("newDate","2026-10-29");move.put("newStartTime","06:00");move.put("newEndTime","07:30");move.put("requestKey","emergency-move");move.remove("incidentId");
        call(coach,"POST","/api/sessions/"+scheduledId+"/reschedule",move,403);
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        call(manager,"POST","/api/sessions/"+scheduledId+"/reschedule",move,200);
        assertFalse(jdbc.queryForObject("SELECT active FROM session_absence_notices WHERE id=?",Boolean.class,notice.get("id").asInt()));
        assertEquals(11,credit());
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
    }
    @Test @Order(7) void externalRefundAndNarrowCashCorrectionKeepOriginalReceiptsAndMoneyPeriods() throws Exception {
        at("2026-11-01T09:00:00");
        var refund=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",lateInvoiceId,"type","Refund","reason","Verified payment after hold expiry"),200);int rid=refund.get("id").asInt();
        call(cashier,"POST","/api/payment-resolutions/"+rid+"/approve",null,403);
        assertEquals("Approved",call(manager,"POST","/api/payment-resolutions/"+rid+"/approve",null,200).get("status").asText());
        var before=call(manager,"GET","/api/reports/revenue?startDate=2026-11-01&endDate=2026-11-30",null,200);assertEquals(0,before.get("moneyRefunded").decimalValue().signum());
        var completion=Map.of("evidence","LOCAL TEST external refund receipt R1","performedAt","2026-11-01T09:00:00","moneyNotReceived",false);
        call(manager,"POST","/api/payment-resolutions/"+rid+"/complete",completion,200);call(manager,"POST","/api/payment-resolutions/"+rid+"/complete",completion,200);
        var bookedOrder=requestOrder("cash-with-history","Cash",member);int historyInvoice=bookedOrder.get("invoiceId").asInt();
        call(cashier,"POST","/api/invoices/"+historyInvoice+"/confirm-cash",Map.of("note","LOCAL TEST actual receipt; booking later cancelled"),200);
        var booking=booking("history-booking",LocalDate.of(2026,11,3),"14:45","16:15");booking.put("subscriptionId",bookedOrder.get("subscriptionId").asInt());
        int bookedId=call(coach,"POST","/api/sessions/book-private",booking,201).get("id").asInt();call(member,"POST","/api/sessions/"+bookedId+"/cancel-private",null,200);
        assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,bookedOrder.get("subscriptionId").asInt()));
        var protectedCase=call(cashier,"POST","/api/payment-resolutions",Map.of("invoiceId",historyInvoice,"type","CashCorrection","reason","History must block automatic reversal"),200);
        assertEquals("Blocked",call(manager,"POST","/api/payment-resolutions/"+protectedCase.get("id").asInt()+"/approve",null,200).get("status").asText());
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
        var resolution=Map.of("endedAt",prior.toString());call(manager,"POST","/api/center-incidents/"+id+"/resolve",resolution,200);assertEquals(3,credit());assertTrue(deadline().isAfter(prior));
        call(manager,"POST","/api/center-incidents/"+id+"/resolve",resolution,200);assertEquals(3,credit());
        var compensation=Map.of("reason","Verified session was not held at all","verifiedNotHeld",true);
        call(manager,"POST","/api/center-incidents/"+id+"/compensate",compensation,200);call(manager,"POST","/api/center-incidents/"+id+"/compensate",compensation,200);assertEquals(4,credit());
        call(manager,"POST","/api/sessions/"+affected+"/reschedule",move("no-history-rewrite",prior.toLocalDate().plusDays(2).toString(),"06:00","07:30","CENTER_INCIDENT",id),400);
        call(coach,"POST","/api/sessions/book-private",booking("rebook-after-recovery",prior.toLocalDate().plusDays(2),"06:00","07:30"),201);assertEquals(3,credit());
        assertEquals(0,jdbc.queryForObject("SELECT expiry_revoked_credit FROM member_packages WHERE subscription_id=?",Integer.class,subscriptionId));
        var extra=booking("approved-minimum",LocalDate.of(2027,1,21),"18:15","19:45");
        call(coach,"POST","/api/sessions/book-private",extra,400);
        extra.put("incidentId",id);extra.put("extraValidityReason","Minimum extension for the agreed replacement slot");extra.put("supportReason","Manager supports the agreed makeup");
        call(manager,"POST","/api/sessions/book-private",extra,201);assertEquals(LocalDateTime.of(2027,1,21,19,45),deadline());assertEquals(2,credit());
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
        at("2027-01-23T10:00:00");subscriptionService.expireOutdatedPackages();
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM member_packages WHERE status='Active'",Integer.class));
        var stillEntitled=call(manager,"GET","/api/reports/members",null,200);assertEquals(1,stillEntitled.get("activeMembers").asInt());assertEquals(1,stillEntitled.get("neverEntitledMembers").asInt());
        at("2027-01-28T09:15:00");
        for(Integer sid:jdbc.queryForList("SELECT session_id FROM sessions WHERE class_id=? ORDER BY session_date,start_time",Integer.class,cid))finalizer.process(sid);
        assertEquals("Completed",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,cid,memberId));
        assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=? AND center_invalidated=false AND attendance_finalized=true",Integer.class,cid));
        var completed=call(manager,"GET","/api/reports/members",null,200);assertEquals(0,completed.get("activeMembers").asInt());assertEquals(1,completed.get("previouslyEntitledMembersWithoutRights").asInt());assertEquals(1,completed.get("neverEntitledMembers").asInt());
    }

    Map<String,String> previewHeader(JsonNode preview){return Map.of("X-Preview-Token",preview.get("previewToken").asText());}
    void toggleHoliday(int id,boolean active) throws Exception{var p=call(manager,"POST","/api/holidays/preview",Map.of("holidayId",id,"active",active),200);call(manager,"PATCH","/api/holidays/"+id+"/toggle",Map.of("active",active),200,previewHeader(p));}
    Map<String,Object> move(String key,String date,String start,String end,String cause,Integer incidentId){var p=new HashMap<String,Object>();p.put("newDate",date);p.put("newStartTime",start);p.put("newEndTime",end);p.put("reason","Agreed and verified schedule change");p.put("cause",cause);p.put("memberAgreed",true);p.put("requestKey",key);if(incidentId!=null)p.put("incidentId",incidentId);return p;}
    void at(String local){clock.current=LocalDateTime.parse(local).atZone(clock.getZone()).toInstant();}
    LocalDateTime deadline(){return jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",LocalDateTime.class,subscriptionId);}
    String callback(JsonNode order) throws Exception {
        TreeMap<String,String> p=new TreeMap<>();p.put("vnp_TmnCode","LOCALTEST");p.put("vnp_TxnRef",order.get("invoiceCode").asText());p.put("vnp_Amount",order.get("amount").decimalValue().movePointRight(2).toPlainString().split("\\.")[0]);p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");p.put("vnp_TransactionNo","LOCAL-"+order.get("invoiceId").asText());
        String q=p.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec("phase-workflow-test-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return q+"&vnp_SecureHash="+HexFormat.of().formatHex(mac.doFinal(q.getBytes(StandardCharsets.UTF_8)));
    }
    @AfterAll void evidence() throws Exception {
        String dir=System.getProperty("scms.evidence-dir","");
        if(!dir.isBlank()) {Path p=Path.of(dir);Files.createDirectories(p);Files.write(p.resolve("http-workflow.txt"),trace,StandardCharsets.UTF_8);
            json.writerWithDefaultPrettyPrinter().writeValue(p.resolve("http-exchanges.json").toFile(),exchanges);}
    }
}
